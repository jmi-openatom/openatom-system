package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.jmi.openatom.server.openatomsystem.common.DocumentServerJwt;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.DocCenterDocument;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.DocCenterDocumentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.DocCenterStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 文档中心：Office 文件上传/列表/在线编辑（ONLYOFFICE Document Server 集成）。 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/document-center")
public class DocCenterController {
  private static final long DOWNLOAD_TOKEN_TTL_SECONDS = 60 * 30;

  private final DocCenterDocumentMapper documentMapper;
  private final DocCenterStorageService storageService;
  private final UserMapper userMapper;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Value("${app.document-server.office-url:https://office.jmi-openatom.cn}")
  private String officeUrl;

  @Value("${app.document-server.callback-base-url:http://host.docker.internal:8921/api/v1}")
  private String callbackBaseUrl;

  @Value("${app.document-server.public-api-base:https://api.jmi-openatom.cn/api/v1}")
  private String publicApiBase;

  @Value("${app.document-server.jwt-secret:}")
  private String jwtSecret;

  /** 文档中心共享库：所有管理员可见全部文档。 */
  @GetMapping
  @SaCheckPermission("document:list")
  public Result<List<DocumentView>> list() {
    List<DocCenterDocument> documents =
        documentMapper.selectList(
            new LambdaQueryWrapper<DocCenterDocument>()
                .orderByDesc(DocCenterDocument::getUpdatedAt));
    List<Integer> ownerIds =
        documents.stream().map(DocCenterDocument::getOwnerUserId).distinct().toList();
    Map<Integer, String> ownerNames = new java.util.HashMap<>();
    if (!ownerIds.isEmpty()) {
      userMapper.selectBatchIds(ownerIds).forEach(
          user -> ownerNames.put(user.getId(), displayName(user)));
    }
    return Result.success(
        documents.stream()
            .map(document -> DocumentView.from(document,
                ownerNames.getOrDefault(document.getOwnerUserId(), "")))
            .toList());
  }

  private static String displayName(User user) {
    return user.getRealName() != null && !user.getRealName().isBlank()
        ? user.getRealName() : user.getUserName();
  }

  public record DocumentView(
      Long id,
      String name,
      String extension,
      Long sizeBytes,
      String ownerName,
      String createdAt,
      String updatedAt) {
    static DocumentView from(DocCenterDocument document, String ownerName) {
      return new DocumentView(
          document.getId(),
          document.getName(),
          document.getExtension(),
          document.getSizeBytes(),
          ownerName,
          document.getCreatedAt() == null ? null : document.getCreatedAt().toString(),
          document.getUpdatedAt() == null ? null : document.getUpdatedAt().toString());
    }
  }

  @PostMapping
  @SaCheckPermission("document:list")
  public Result<DocCenterDocument> upload(@RequestParam("file") MultipartFile file) {
    try {
      DocCenterDocument document = storageService.store(file);
      document.setOwnerUserId(StpUtil.getLoginIdAsInt());
      documentMapper.insert(document);
      return Result.success(document);
    } catch (IllegalArgumentException exception) {
      return Result.error(400, messageOf(exception.getMessage()));
    } catch (IOException exception) {
      log.warn("document upload failed: {}", exception.getMessage());
      return Result.error(500, "文档保存失败，请稍后重试");
    }
  }

  @DeleteMapping("/{documentId}")
  @SaCheckPermission("document:list")
  public Result<String> delete(@PathVariable Long documentId) {
    DocCenterDocument document = findOr404(documentId);
    documentMapper.deleteById(documentId);
    storageService.delete(document);
    return Result.success("文档已删除");
  }

  /** 登录用户下载（普通场景）。 */
  @GetMapping("/{documentId}/download")
  @SaCheckPermission("document:list")
  public ResponseEntity<Resource> download(@PathVariable Long documentId) {
    DocCenterDocument document = findOr404(documentId);
    return streamFile(document);
  }

  /**
   * Document Server 拉取文档内容。无登录态，使用短期签名 token
   * （公开路径，见 SaTokenConfigure 白名单）。
   */
  @GetMapping("/{documentId}/file")
  public ResponseEntity<Resource> file(
      @PathVariable Long documentId,
      @RequestParam(value = "token", required = false) String token) {
    JsonNode claim;
    try {
      claim = DocumentServerJwt.verify(jwtSecret, token);
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "document_server_not_configured");
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_token");
    }
    String expected = "doc:" + documentId + ":";
    String value = claim.isTextual() ? claim.asText() : "";
    if (!value.startsWith(expected)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_token");
    }
    long expiresAt = Long.parseLong(value.substring(expected.length()));
    if (expiresAt < System.currentTimeMillis() / 1000) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "token_expired");
    }
    DocCenterDocument document = findOr404(documentId);
    return streamFile(document);
  }

  /** 生成 ONLYOFFICE 编辑器配置（整份 config 用 JWT 签名后返回）。 */
  @PostMapping("/{documentId}/edit-config")
  @SaCheckPermission("document:list")
  public Result<ObjectNode> editConfig(@PathVariable Long documentId) {
    DocCenterDocument document = findOr404(documentId);
    try {
      return Result.success(buildEditorConfig(document));
    } catch (IllegalStateException exception) {
      return Result.error(503, "文档编辑服务未配置，请先在主站环境配置 DOCUMENT_SERVER_JWT_SECRET");
    }
  }

  private ObjectNode buildEditorConfig(DocCenterDocument document) {
    String fileType = document.getExtension();
    String documentType = switch (fileType) {
      case "xlsx" -> "spreadsheet";
      case "pptx" -> "presentation";
      default -> "word";
    };
    String key = document.getId() + "_"
        + (document.getUpdatedAt() == null ? document.getCreatedAt() : document.getUpdatedAt()).getTime();
    String downloadUrl = publicApiBase
        + "/document-center/" + document.getId() + "/file?token="
        + downloadToken(document.getId());
    String callbackUrl = callbackBaseUrl + "/document-center/" + document.getId() + "/callback";
    Integer userId = StpUtil.getLoginIdAsInt();
    User owner = userMapper.selectById(userId);

    ObjectNode user = objectMapper.createObjectNode();
    user.put("id", String.valueOf(userId));
    user.put("name", owner != null && owner.getRealName() != null && !owner.getRealName().isBlank()
        ? owner.getRealName() : "成员");

    ObjectNode permissions = objectMapper.createObjectNode();
    permissions.put("edit", true);
    permissions.put("download", true);
    permissions.put("print", true);

    ObjectNode documentNode = objectMapper.createObjectNode();
    documentNode.put("fileType", fileType);
    documentNode.put("key", key);
    documentNode.put("title", document.getName());
    documentNode.put("url", downloadUrl);
    documentNode.set("permissions", permissions);

    ObjectNode editorConfig = objectMapper.createObjectNode();
    editorConfig.put("callbackUrl", callbackUrl);
    editorConfig.put("lang", "zh-CN");
    editorConfig.put("mode", "edit");
    editorConfig.set("user", user);

    ObjectNode config = objectMapper.createObjectNode();
    config.set("document", documentNode);
    config.put("documentType", documentType);
    config.set("editorConfig", editorConfig);
    config.put("height", "100%");
    config.put("width", "100%");
    config.put("type", "desktop");

    String token = DocumentServerJwt.sign(jwtSecret, config);
    config.put("token", token);

    ObjectNode response = objectMapper.createObjectNode();
    response.put("documentServerUrl", officeUrl);
    response.set("config", config);
    return response;
  }

  /**
   * Document Server 保存回调（公开路径，JWT 校验）。
   * status=2 表示文档已保存，下载新内容并覆盖存储。
   */
  @PostMapping("/{documentId}/callback")
  public ResponseEntity<ObjectNode> callback(
      @PathVariable Long documentId,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody(required = false) JsonNode body,
      HttpServletRequest request) {
    if (!verifyCallbackJwt(authorization, body)) {
      return forbidden();
    }
    ObjectNode result = objectMapper.createObjectNode();
    try {
      int status = body == null ? 0 : body.path("status").asInt(0);
      if (status == 2 || status == 3) {
        String url = body.path("url").asText("");
        if (!url.isBlank()) {
          HttpRequest download =
              HttpRequest.newBuilder(URI.create(url))
                  .timeout(Duration.ofSeconds(60))
                  .header("Accept", "application/octet-stream")
                  .GET()
                  .build();
          HttpResponse<byte[]> response =
              httpClient.send(download, HttpResponse.BodyHandlers.ofByteArray());
          if (response.statusCode() >= 200 && response.statusCode() < 300
              && response.body().length > 0) {
            DocCenterDocument document = findOr404(documentId);
            storageService.overwrite(document, response.body());
            document.setSizeBytes((long) response.body().length);
            documentMapper.updateById(document);
            log.info("document {} saved via ONLYOFFICE callback ({} bytes)",
                documentId, response.body().length);
          }
        }
      }
      result.put("error", 0);
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    } catch (Exception exception) {
      log.warn("document {} callback failed: {}", documentId, exception.getMessage());
      result.put("error", 1);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(result);
    }
  }

  private boolean verifyCallbackJwt(String authorization, JsonNode body) {
    String token = null;
    if (authorization != null && authorization.startsWith("Bearer ")) {
      token = authorization.substring(7).trim();
    }
    if (token == null && body != null && body.has("token")) {
      token = body.get("token").asText(null);
    }
    if (token == null) {
      return false;
    }
    try {
      DocumentServerJwt.verify(jwtSecret, token);
      return true;
    } catch (IllegalStateException exception) {
      log.warn("document callback rejected: {}", exception.getMessage());
      return false;
    } catch (IllegalArgumentException exception) {
      log.warn("document callback rejected: {}", exception.getMessage());
      return false;
    }
  }

  private ResponseEntity<ObjectNode> forbidden() {
    ObjectNode result = objectMapper.createObjectNode();
    result.put("error", 1);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body(result);
  }

  private ResponseEntity<Resource> streamFile(DocCenterDocument document) {
    Optional<Resource> resource = storageService.load(document);
    if (resource.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document_missing");
    }
    return ResponseEntity.ok()
        .contentType(contentTypeOf(document.getExtension()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename*=UTF-8''" + encodeName(document.getName()))
        .body(resource.get());
  }

  private MediaType contentTypeOf(String extension) {
    return switch (extension) {
      case "xlsx" -> MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      case "pptx" -> MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.presentationml.presentation");
      default -> MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    };
  }

  private String encodeName(String name) {
    try {
      return java.net.URLEncoder.encode(name == null ? "document" : name, StandardCharsets.UTF_8);
    } catch (Exception exception) {
      return "document";
    }
  }

  private String downloadToken(Long documentId) {
    long expiresAt = System.currentTimeMillis() / 1000 + DOWNLOAD_TOKEN_TTL_SECONDS;
    return DocumentServerJwt.signString(jwtSecret, "doc:" + documentId + ":" + expiresAt);
  }

  private DocCenterDocument findOr404(Long documentId) {
    DocCenterDocument document =
        documentId == null ? null : documentMapper.selectById(documentId);
    if (document == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document_not_found");
    }
    return document;
  }

  private String messageOf(String code) {
    return switch (code == null ? "" : code) {
      case "document_empty" -> "请选择要上传的文件。";
      case "document_too_large" -> "单个文档不能超过 50 MiB。";
      case "document_type_not_allowed" -> "仅支持 .docx、.xlsx、.pptx 文件。";
      default -> "文档上传失败，请稍后重试。";
    };
  }
}
