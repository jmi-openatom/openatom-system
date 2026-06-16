package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveDocumentTemplateVariablesDTO;
import edu.jmi.openatom.server.openatomsystem.entity.DocumentTemplate;
import edu.jmi.openatom.server.openatomsystem.entity.GeneratedDocument;
import edu.jmi.openatom.server.openatomsystem.service.DocumentTemplateService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

/** docx 模板与生成文档控制器。 */
@RestController
@RequiredArgsConstructor
public class DocumentTemplateController {
  private final DocumentTemplateService documentTemplateService;

  @PostMapping("/document-templates")
  @SaCheckPermission("document:create")
  public Result<DocumentTemplate> upload(
      @RequestParam String templateType,
      @RequestParam String templateName,
      @RequestParam MultipartFile file) {
    return documentTemplateService.upload(templateType, templateName, file);
  }

  @GetMapping("/document-templates")
  @SaCheckPermission("document:list")
  public Result<List<DocumentTemplate>> list(
      @RequestParam(required = false) String templateType,
      @RequestParam(required = false) String status) {
    return documentTemplateService.list(templateType, status);
  }

  @GetMapping("/document-templates/{templateId}/variables")
  @SaCheckPermission("document:list")
  public Result<List<Map<String, Object>>> variables(@PathVariable Long templateId) {
    return documentTemplateService.variables(templateId);
  }

  @PutMapping("/document-templates/{templateId}/variables")
  @SaCheckPermission("document:update")
  public Result<String> saveVariables(
      @PathVariable Long templateId,
      @Valid @RequestBody RequestSaveDocumentTemplateVariablesDTO request) {
    return documentTemplateService.saveVariables(templateId, request);
  }

  @GetMapping("/generated-documents/{documentId}/download")
  @SaCheckPermission("document:export")
  public ResponseEntity<byte[]> download(@PathVariable Long documentId) {
    GeneratedDocument document = documentTemplateService.generatedDocument(documentId);
    if (document == null) return ResponseEntity.notFound().build();
    byte[] bytes = documentTemplateService.downloadGenerated(documentId);
    String fileName = URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .body(bytes);
  }
}
