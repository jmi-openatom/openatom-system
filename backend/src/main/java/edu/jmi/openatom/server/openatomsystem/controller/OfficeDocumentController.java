package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestSaveOfficeDocumentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseOfficeDocumentUserOptionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.OfficeDocument;
import edu.jmi.openatom.server.openatomsystem.service.OfficeDocumentService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公文管理控制器
 *
 * <p>提供公文的列表查询, 创建, 更新, 用户选项查询及导出 Word 文档等功能
 */
@RestController
@RequiredArgsConstructor
public class OfficeDocumentController {
  private final OfficeDocumentService officeDocumentService;

  /**
   * 查询公文列表
   *
   * @param docType 公文类型（可选）
   * @param keyword 搜索关键词（可选）
   * @return 公文列表
   */
  @GetMapping("/office-documents")
  @SaCheckPermission("document:list")
  public ApiResponse<List<OfficeDocument>> list(
      @RequestParam(required = false) String docType,
      @RequestParam(required = false) String keyword) {
    return officeDocumentService.list(docType, keyword);
  }

  /**
   * 获取公文用户选项列表
   *
   * @param keyword 搜索关键词（可选）
   * @return 用户选项列表
   */
  @GetMapping("/office-documents/user-options")
  @SaCheckPermission("document:list")
  public ApiResponse<List<ResponseOfficeDocumentUserOptionDTO>> userOptions(
      @RequestParam(required = false) String keyword) {
    return officeDocumentService.listUserOptions(keyword);
  }

  /**
   * 创建公文
   *
   * @param request 创建公文请求参数
   * @return 公文ID
   */
  @PostMapping("/office-documents")
  @SaCheckPermission("document:create")
  public ApiResponse<Integer> create(@Valid @RequestBody RequestSaveOfficeDocumentDTO request) {
    return officeDocumentService.create(request);
  }

  /**
   * 更新公文
   *
   * @param documentId 公文ID
   * @param request 更新公文请求参数
   * @return 更新结果
   */
  @PatchMapping("/office-documents/{documentId}")
  @SaCheckPermission("document:update")
  public ApiResponse<String> update(
      @PathVariable Integer documentId, @Valid @RequestBody RequestSaveOfficeDocumentDTO request) {
    return officeDocumentService.update(documentId, request);
  }

  /**
   * 导出公文为 Word 文档
   *
   * @param documentId 公文ID
   * @return Word 文件字节流
   */
  @GetMapping("/office-documents/{documentId}/export")
  @SaCheckPermission("document:export")
  public ResponseEntity<byte[]> export(@PathVariable Integer documentId) {
    byte[] bytes = officeDocumentService.exportDocx(documentId);
    String fileName =
        URLEncoder.encode("office-document-" + documentId + ".docx", StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .body(bytes);
  }
}
