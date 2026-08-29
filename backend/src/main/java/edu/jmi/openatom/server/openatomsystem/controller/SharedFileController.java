package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.jmi.openatom.server.openatomsystem.common.DocumentServerJwt;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.SharedFile;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.SharedFileMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
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
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/** 共享文件架：目录 + 任意文件，支持密码保护与分类预览/在线编辑。 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shared-files")
public class SharedFileController {
  private static final long DOWNLOAD_TOKEN_TTL_SECONDS = 60 * 30;
  private static final long MAX_FILE_SIZE = 500L * 1024 * 1024;
  private static final Set<String> IMAGE_EXTENSIONS =
      Set.of("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico", "avif");
  private static final Set<String> TEXT_EXTENSIONS = Set.of("md", "markdown", "txt", "text");

  private static final Set<String> OFFICE_WORD =
      Set.of("docx", "doc", "odt", "rtf", "txt");
  private static final Set<String> OFFICE_CELL = Set.of("xlsx", "xls", "ods", "csv");
  private static final Set<String> OFFICE_SLIDE = Set.of("pptx", "ppt", "odp");
  private static final Set<String> OFFICE_EXTS;

  private static final Set<String> CREATABLE_TYPES =
      Set.of("md", "docx", "xlsx", "pptx");
  private static final Set<String> KNOWN_DOC_EXTS =
      Set.of("md", "markdown", "docx", "doc", "odt", "rtf", "txt", "text",
          "xlsx", "xls", "ods", "csv", "pptx", "ppt", "odp");

  private static final Map<String, String> NEW_FILE_DEFAULT_NAMES =
      Map.of(
          "md", "未命名文档.md",
          "docx", "未命名文档.docx",
          "xlsx", "未命名表格.xlsx",
          "pptx", "未命名演示文稿.pptx");

  static {
    java.util.HashSet<String> all = new java.util.HashSet<>();
    all.addAll(OFFICE_WORD);
    all.addAll(OFFICE_CELL);
    all.addAll(OFFICE_SLIDE);
    OFFICE_EXTS = java.util.Collections.unmodifiableSet(all);
  }

  private final SharedFileMapper fileMapper;
  private final UserMapper userMapper;
  private final PasswordService passwordService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Value("${app.shared-files.storage-dir:./uploads/shared-files}")
  private String storageDir;

  @Value("${app.document-server.office-url:https://office.jmi-openatom.cn}")
  private String officeUrl;

  @Value("${app.document-server.callback-base-url:https://api.jmi-openatom.cn/api/v1}")
  private String callbackBaseUrl;

  @Value("${app.document-server.public-api-base:https://api.jmi-openatom.cn/api/v1}")
  private String publicApiBase;

  @Value("${app.document-server.jwt-secret:}")
  private String jwtSecret;

  /** 列出目录内容。 */
  @GetMapping
  @SaCheckPermission("document:list")
  public Result<List<FileView>> list(@RequestParam(required = false) Long parentId) {
    LambdaQueryWrapper<SharedFile> wrapper = new LambdaQueryWrapper<>();
    if (parentId == null) {
      wrapper.isNull(SharedFile::getParentId);
    } else {
      wrapper.eq(SharedFile::getParentId, parentId);
    }
    List<SharedFile> children =
        fileMapper.selectList(
            wrapper.orderByDesc(SharedFile::getDir).orderByAsc(SharedFile::getName));
    return Result.success(children.stream().map(FileView::from).toList());
  }

  /** 目录祖先链（面包屑）。 */
  @GetMapping("/path")
  @SaCheckPermission("document:list")
  public Result<List<PathItem>> path(@RequestParam(required = false) Long fileId) {
    List<PathItem> chain = new ArrayList<>();
    Long cursor = fileId;
    int guard = 0;
    while (cursor != null && guard++ < 50) {
      SharedFile current = fileMapper.selectById(cursor);
      if (current == null) {
        break;
      }
      chain.add(new PathItem(current.getId(), current.getName()));
      cursor = current.getParentId();
    }
    java.util.Collections.reverse(chain);
    return Result.success(chain);
  }

  /** 移动到其他目录（拖拽）。 */
  @PostMapping("/{fileId}/move")
  @SaCheckPermission("document:list")
  public Result<String> move(@PathVariable Long fileId, @RequestBody MoveRequest request) {
    SharedFile file = requireFile(fileId);
    ensureParent(request.parentId());
    if (file.getDir() && request.parentId() != null) {
      List<Long> descendants = new ArrayList<>();
      List<Long> frontier = new ArrayList<>(List.of(file.getId()));
      while (!frontier.isEmpty()) {
        List<Long> next = new ArrayList<>();
        for (Long parent : frontier) {
          fileMapper.selectList(
                  new LambdaQueryWrapper<SharedFile>().eq(SharedFile::getParentId, parent))
              .forEach(child -> next.add(child.getId()));
        }
        descendants.addAll(next);
        frontier = next;
      }
      if (descendants.contains(request.parentId())) {
        return Result.error(400, "不能移动到自身或其子目录");
      }
    }
    if (nameExists(request.parentId(), file.getName(), file.getDir())) {
      return Result.error(400, "目标目录已存在同名项");
    }
    file.setParentId(request.parentId());
    fileMapper.updateById(file);
    return Result.success("已移动");
  }

  /** 创建目录。 */
  @PostMapping("/dir")
  @SaCheckPermission("document:list")
  public Result<SharedFile> createDir(@RequestBody DirRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      return Result.error(400, "目录名不能为空");
    }
    String name = request.name().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    if (name.length() > 200) {
      return Result.error(400, "目录名过长");
    }
    ensureParent(request.parentId());
    if (nameExists(request.parentId(), name, true)) {
      return Result.error(400, "同级目录已存在同名目录");
    }
    SharedFile dir =
        SharedFile.builder()
            .parentId(request.parentId())
            .name(name)
            .dir(true)
            .extension("")
            .sizeBytes(0L)
            .ownerUserId(StpUtil.getLoginIdAsInt())
            .passwordHash(encodePassword(request.password()))
            .build();
    fileMapper.insert(dir);
    return Result.success(dir);
  }

  /** 新建空白文件（markdown/word/excel/ppt）。 */
  @PostMapping("/create")
  @SaCheckPermission("document:list")
  public Result<SharedFile> createFile(@RequestBody CreateFileRequest request) {
    String type = request.type() == null ? null : request.type().toLowerCase(Locale.ROOT);
    if (type == null || !CREATABLE_TYPES.contains(type)) {
      return Result.error(400, "仅支持新建 md、docx、xlsx 或 pptx 文件");
    }
    ensureParent(request.parentId());
    String name = resolveNewFileName(request.name(), type);
    if (nameExists(request.parentId(), name, false)) {
      return Result.error(400, "同级已存在同名文件");
    }
    String storageName = UUID.randomUUID() + "." + type;
    Path target = root().resolve(storageName).normalize();
    if (!target.getParent().equals(root())) {
      return Result.error(500, "文件名不合法");
    }
    try {
      Files.createDirectories(root());
      Files.write(target, initialFileContent(type, name));
    } catch (IOException exception) {
      log.warn("shared file create failed: {}", exception.getMessage());
      return Result.error(500, "文件创建失败，请稍后重试");
    }
    SharedFile entity =
        SharedFile.builder()
            .parentId(request.parentId())
            .name(name)
            .dir(false)
            .extension(type)
            .sizeBytes(safeSize(target))
            .storageName(storageName)
            .ownerUserId(StpUtil.getLoginIdAsInt())
            .passwordHash(encodePassword(request.password()))
            .build();
    fileMapper.insert(entity);
    return Result.success(entity);
  }

  /** 上传文件（任意类型）。 */
  @PostMapping("/upload")
  @SaCheckPermission("document:list")
  public Result<SharedFile> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "parentId", required = false) Long parentId,
      @RequestParam(value = "password", required = false) String password) {
    if (file == null || file.isEmpty()) {
      return Result.error(400, "请选择要上传的文件");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      return Result.error(400, "单个文件不能超过 500 MiB");
    }
    ensureParent(parentId);
    String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
    String name = sanitizeName(original);
    if (nameExists(parentId, name, false)) {
      return Result.error(400, "同级已存在同名文件");
    }
    String extension = extensionOf(name);
    String storageName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
    Path target = root().resolve(storageName).normalize();
    if (!target.getParent().equals(root())) {
      return Result.error(500, "文件名不合法");
    }
    try {
      Files.createDirectories(root());
      try (var input = file.getInputStream()) {
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException exception) {
      log.warn("shared file upload failed: {}", exception.getMessage());
      return Result.error(500, "文件保存失败，请稍后重试");
    }
    SharedFile entity =
        SharedFile.builder()
            .parentId(parentId)
            .name(name)
            .dir(false)
            .extension(extension)
            .sizeBytes(file.getSize())
            .storageName(storageName)
            .ownerUserId(StpUtil.getLoginIdAsInt())
            .passwordHash(encodePassword(password))
            .build();
    fileMapper.insert(entity);
    return Result.success(entity);
  }

  /** 重命名。 */
  @PostMapping("/{fileId}/rename")
  @SaCheckPermission("document:list")
  public Result<String> rename(@PathVariable Long fileId, @RequestBody RenameRequest request) {
    SharedFile file = requireFile(fileId);
    if (request.name() == null || request.name().isBlank()) {
      return Result.error(400, "名称不能为空");
    }
    String name = sanitizeName(request.name());
    if (nameExists(file.getParentId(), name, file.getDir())) {
      return Result.error(400, "同级已存在同名项");
    }
    file.setName(name);
    if (!file.getDir()) {
      file.setExtension(extensionOf(name));
    }
    fileMapper.updateById(file);
    return Result.success("已重命名");
  }

  /** 设置/清除访问密码。 */
  @PostMapping("/{fileId}/password")
  @SaCheckPermission("document:list")
  public Result<String> setPassword(@PathVariable Long fileId, @RequestBody PasswordRequest request) {
    SharedFile file = requireFile(fileId);
    if (request.password() == null || request.password().isBlank()) {
      file.setPasswordHash(null);
    } else {
      if (request.password().length() > 64) {
        return Result.error(400, "密码过长（最多 64 位）");
      }
      file.setPasswordHash(passwordService.encode(request.password()));
    }
    fileMapper.updateById(file);
    return Result.success("密码已更新");
  }

  /** 删除（目录递归）。 */
  @DeleteMapping("/{fileId}")
  @SaCheckPermission("document:list")
  public Result<String> delete(@PathVariable Long fileId) {
    SharedFile file = requireFile(fileId);
    List<SharedFile> all = new ArrayList<>();
    all.add(file);
    int index = 0;
    while (index < all.size()) {
      SharedFile current = all.get(index++);
      if (current.getDir()) {
        all.addAll(
            fileMapper.selectList(
                new LambdaQueryWrapper<SharedFile>().eq(SharedFile::getParentId, current.getId())));
      }
    }
    for (SharedFile item : all) {
      fileMapper.deleteById(item.getId());
      if (!item.getDir() && item.getStorageName() != null && !item.getStorageName().isBlank()) {
        try {
          Files.deleteIfExists(root().resolve(item.getStorageName()).normalize());
        } catch (IOException ignored) {
          // 磁盘清理失败不影响记录删除
        }
      }
    }
    return Result.success("已删除");
  }

  /** 下载（需密码）。 */
  @GetMapping("/{fileId}/download")
  @SaCheckPermission("document:list")
  public ResponseEntity<Resource> download(
      @PathVariable Long fileId, @RequestParam(required = false) String password) {
    SharedFile file = requireFile(fileId);
    checkAccess(file, password);
    return streamFile(file);
  }

  /**
   * ONLYOFFICE 专用下载端点（公开白名单，仅接受短期签名 token，无登录态）。
   * Document Server 无法携带主站登录态，走此端点拉取文档内容。
   */
  @GetMapping("/{fileId}/raw")
  public ResponseEntity<Resource> raw(
      @PathVariable Long fileId, @RequestParam(required = false) String token) {
    verifyDownloadToken(token, fileId);
    return streamFile(requireFile(fileId));
  }

  /**
   * 文件内容（预览 + ONLYOFFICE 下载共用）。
   * 预览：?password=；ONLYOFFICE：?token=（短期签名）。
   */
  @GetMapping("/{fileId}/content")
  @SaCheckPermission("document:list")
  public ResponseEntity<Resource> content(
      @PathVariable Long fileId,
      @RequestParam(required = false) String password,
      @RequestParam(required = false) String token) {
    SharedFile file = requireFile(fileId);
    if (token != null && !token.isBlank()) {
      verifyDownloadToken(token, fileId);
    } else {
      checkAccess(file, password);
    }
    return streamFile(file);
  }

  /** Markdown 等文本内容（预览渲染用）。 */
  @GetMapping("/{fileId}/text")
  @SaCheckPermission("document:list")
  public Result<String> text(
      @PathVariable Long fileId, @RequestParam(required = false) String password) {
    SharedFile file = requireFile(fileId);
    checkAccess(file, password);
    if (file.getDir() || !TEXT_EXTENSIONS.contains(file.getExtension())) {
      return Result.error(400, "该文件不支持文本预览");
    }
    try {
      return Result.success(Files.readString(root().resolve(file.getStorageName()).normalize()));
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file_missing");
    }
  }

  /** ONLYOFFICE 编辑配置（docx/xlsx/pptx）。 */
  @PostMapping("/{fileId}/edit-config")
  @SaCheckPermission("document:list")
  public Result<ObjectNode> editConfig(
      @PathVariable Long fileId, @RequestParam(required = false) String password) {
    SharedFile file = requireFile(fileId);
    checkAccess(file, password);
    if (file.getDir() || !isOffice(file.getExtension())) {
      return Result.error(400, "该文件不支持在线编辑");
    }
    try {
      return Result.success(buildEditorConfig(file));
    } catch (IllegalStateException exception) {
      return Result.error(503, "文档编辑服务未配置，请先在主站环境配置 DOCUMENT_SERVER_JWT_SECRET");
    }
  }

  /** ONLYOFFICE 保存回调（公开路径，JWT 校验，见 SaTokenConfigure 白名单）。 */
  @PostMapping("/{fileId}/callback")
  public ResponseEntity<ObjectNode> callback(
      @PathVariable Long fileId,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody(required = false) JsonNode body) {
    ObjectNode result = objectMapper.createObjectNode();
    if (!verifyCallbackJwt(authorization, body)) {
      result.put("error", 1);
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .contentType(MediaType.APPLICATION_JSON).body(result);
    }
    try {
      int status = body == null ? 0 : body.path("status").asInt(0);
      if (status == 2 || status == 3) {
        String url = body.path("url").asText("");
        if (!url.isBlank()) {
          HttpResponse<byte[]> response =
              httpClient.send(
                  HttpRequest.newBuilder(URI.create(url))
                      .timeout(Duration.ofSeconds(120))
                      .header("Accept", "application/octet-stream")
                      .GET()
                      .build(),
                  HttpResponse.BodyHandlers.ofByteArray());
          if (response.statusCode() >= 200 && response.statusCode() < 300
              && response.body().length > 0) {
            SharedFile file = requireFile(fileId);
            Files.write(root().resolve(file.getStorageName()).normalize(), response.body());
            file.setSizeBytes((long) response.body().length);
            fileMapper.updateById(file);
            log.info("shared file {} saved via ONLYOFFICE ({} bytes)", fileId, response.body().length);
          }
        }
      }
      result.put("error", 0);
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    } catch (Exception exception) {
      log.warn("shared file {} callback failed: {}", fileId, exception.getMessage());
      result.put("error", 1);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON).body(result);
    }
  }

  // ===== 内部工具 =====

  private ObjectNode buildEditorConfig(SharedFile file) {
    String fileType = file.getExtension();
    String documentType;
    if (OFFICE_CELL.contains(fileType)) {
      documentType = "spreadsheet";
    } else if (OFFICE_SLIDE.contains(fileType)) {
      documentType = "presentation";
    } else {
      documentType = "word";
    }
    String key = file.getId() + "_"
        + (file.getUpdatedAt() == null ? file.getCreatedAt() : file.getUpdatedAt()).getTime();
    String downloadUrl = publicApiBase + "/shared-files/" + file.getId() + "/raw?token="
        + downloadToken(file.getId());
    String callbackUrl = callbackBaseUrl + "/shared-files/" + file.getId() + "/callback";
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
    documentNode.put("title", file.getName());
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
    } catch (Exception exception) {
      return false;
    }
  }

  private void verifyDownloadToken(String token, Long fileId) {
    try {
      JsonNode claim = DocumentServerJwt.verify(jwtSecret, token);
      String expected = "file:" + fileId + ":";
      String value = claim.isTextual() ? claim.asText() : "";
      if (!value.startsWith(expected)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_token");
      }
      long expiresAt = Long.parseLong(value.substring(expected.length()));
      if (expiresAt < System.currentTimeMillis() / 1000) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "token_expired");
      }
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "document_server_not_configured");
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_token");
    }
  }

  private String downloadToken(Long fileId) {
    long expiresAt = System.currentTimeMillis() / 1000 + DOWNLOAD_TOKEN_TTL_SECONDS;
    return DocumentServerJwt.signString(jwtSecret, "file:" + fileId + ":" + expiresAt);
  }

  private void checkAccess(SharedFile file, String password) {
    if (file.getPasswordHash() == null || file.getPasswordHash().isBlank()) {
      return;
    }
    if (password == null || !passwordService.matches(password, file.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "password_required");
    }
  }

  private String encodePassword(String password) {
    if (password == null || password.isBlank()) {
      return null;
    }
    return passwordService.encode(password);
  }

  private void ensureParent(Long parentId) {
    if (parentId == null) {
      return;
    }
    SharedFile parent = fileMapper.selectById(parentId);
    if (parent == null || !parent.getDir()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parent_not_found");
    }
  }

  private boolean nameExists(Long parentId, String name, boolean dir) {
    LambdaQueryWrapper<SharedFile> wrapper =
        new LambdaQueryWrapper<SharedFile>()
            .eq(SharedFile::getName, name)
            .eq(SharedFile::getDir, dir);
    if (parentId == null) {
      wrapper.isNull(SharedFile::getParentId);
    } else {
      wrapper.eq(SharedFile::getParentId, parentId);
    }
    Long count = fileMapper.selectCount(wrapper);
    return count != null && count > 0;
  }

  private SharedFile requireFile(Long fileId) {
    SharedFile file = fileId == null ? null : fileMapper.selectById(fileId);
    if (file == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file_not_found");
    }
    return file;
  }

  private ResponseEntity<Resource> streamFile(SharedFile file) {
    if (file.getDir() || file.getStorageName() == null || file.getStorageName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "not_a_file");
    }
    Path target = root().resolve(file.getStorageName()).normalize();
    if (!target.getParent().equals(root()) || !Files.isRegularFile(target)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file_missing");
    }
    Resource resource = new FileSystemResource(target);
    return ResponseEntity.ok()
        .contentType(contentTypeOf(file.getExtension()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename*=UTF-8''" + encodeName(file.getName()))
        .body(resource);
  }

  private MediaType contentTypeOf(String extension) {
    String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    return switch (ext) {
      case "png" -> MediaType.IMAGE_PNG;
      case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
      case "gif" -> MediaType.IMAGE_GIF;
      case "webp" -> MediaType.parseMediaType("image/webp");
      case "svg" -> MediaType.parseMediaType("image/svg+xml");
      case "pdf" -> MediaType.APPLICATION_PDF;
      case "md", "markdown", "txt", "text" -> MediaType.TEXT_PLAIN;
      case "xlsx" -> MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      case "pptx" -> MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.presentationml.presentation");
      case "docx" -> MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
      default -> MediaType.APPLICATION_OCTET_STREAM;
    };
  }

  private boolean isOffice(String extension) {
    return extension != null && OFFICE_EXTS.contains(extension.toLowerCase(Locale.ROOT));
  }

  public static boolean isImage(String extension) {
    return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
  }

  public static boolean isText(String extension) {
    return extension != null && TEXT_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
  }

  private String sanitizeName(String name) {
    String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    return cleaned.isBlank() ? "unnamed" : cleaned;
  }

  private String extensionOf(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  private String resolveNewFileName(String name, String type) {
    String base = name == null || name.isBlank()
        ? NEW_FILE_DEFAULT_NAMES.get(type)
        : sanitizeName(name);
    int dot = base.lastIndexOf('.');
    if (dot > 0 && KNOWN_DOC_EXTS.contains(base.substring(dot + 1).toLowerCase(Locale.ROOT))) {
      base = base.substring(0, dot);
    }
    base = base.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    if (base.isBlank()) {
      base = NEW_FILE_DEFAULT_NAMES.get(type);
      base = base.substring(0, base.lastIndexOf('.'));
    }
    return base + "." + type;
  }

  private byte[] initialFileContent(String type, String name) throws IOException {
    String baseName = name.substring(0, name.lastIndexOf('.'));
    return switch (type) {
      case "md" -> ("# " + baseName + "\n\n在这里开始写作…\n").getBytes(StandardCharsets.UTF_8);
      case "docx" -> {
        try (XWPFDocument document = new XWPFDocument();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          XWPFParagraph paragraph = document.createParagraph();
          XWPFRun run = paragraph.createRun();
          run.setText(baseName);
          document.write(out);
          yield out.toByteArray();
        }
      }
      case "xlsx" -> {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          workbook.createSheet("Sheet1");
          workbook.write(out);
          yield out.toByteArray();
        }
      }
      case "pptx" -> {
        try (XMLSlideShow slideshow = new XMLSlideShow();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          slideshow.createSlide();
          slideshow.write(out);
          yield out.toByteArray();
        }
      }
      default -> throw new IllegalArgumentException("unsupported file type: " + type);
    };
  }

  private long safeSize(Path target) {
    try {
      return Files.size(target);
    } catch (IOException ignored) {
      return 0L;
    }
  }

  private String encodeName(String name) {
    try {
      return java.net.URLEncoder.encode(name == null ? "file" : name, StandardCharsets.UTF_8);
    } catch (Exception exception) {
      return "file";
    }
  }

  private Path root() {
    return Path.of(storageDir.trim()).toAbsolutePath().normalize();
  }

  public record FileView(
      Long id,
      Long parentId,
      String name,
      Boolean dir,
      String extension,
      Long sizeBytes,
      Boolean hasPassword,
      String createdAt,
      String updatedAt) {
    static FileView from(SharedFile file) {
      return new FileView(
          file.getId(),
          file.getParentId(),
          file.getName(),
          file.getDir(),
          file.getExtension(),
          file.getSizeBytes(),
          file.getPasswordHash() != null && !file.getPasswordHash().isBlank(),
          file.getCreatedAt() == null ? null : file.getCreatedAt().toString(),
          file.getUpdatedAt() == null ? null : file.getUpdatedAt().toString());
    }
  }

  public record MoveRequest(Long parentId) {}

  public record PathItem(Long id, String name) {}

  public record DirRequest(Long parentId, String name, String password) {}

  public record CreateFileRequest(Long parentId, String name, String type, String password) {}

  public record RenameRequest(String name) {}

  public record PasswordRequest(String password) {}
}
