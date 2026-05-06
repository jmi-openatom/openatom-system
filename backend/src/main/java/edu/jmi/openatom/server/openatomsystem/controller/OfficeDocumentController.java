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

@RestController
@RequiredArgsConstructor
public class OfficeDocumentController {
  private final OfficeDocumentService officeDocumentService;

  @GetMapping("/office-documents")
  @SaCheckPermission("document:list")
  public ApiResponse<List<OfficeDocument>> list(
      @RequestParam(required = false) String docType,
      @RequestParam(required = false) String keyword) {
    return officeDocumentService.list(docType, keyword);
  }

  @GetMapping("/office-documents/user-options")
  @SaCheckPermission("document:list")
  public ApiResponse<List<ResponseOfficeDocumentUserOptionDTO>> userOptions(
      @RequestParam(required = false) String keyword) {
    return officeDocumentService.listUserOptions(keyword);
  }

  @PostMapping("/office-documents")
  @SaCheckPermission("document:create")
  public ApiResponse<Integer> create(@Valid @RequestBody RequestSaveOfficeDocumentDTO request) {
    return officeDocumentService.create(request);
  }

  @PatchMapping("/office-documents/{documentId}")
  @SaCheckPermission("document:update")
  public ApiResponse<String> update(
      @PathVariable Integer documentId, @Valid @RequestBody RequestSaveOfficeDocumentDTO request) {
    return officeDocumentService.update(documentId, request);
  }

  @GetMapping("/office-documents/{documentId}/export")
  @SaCheckPermission("document:export")
  public ResponseEntity<byte[]> export(@PathVariable Integer documentId) {
    byte[] bytes = officeDocumentService.exportDocx(documentId);
    String fileName =
        URLEncoder.encode("office-document-" + documentId + ".docx", StandardCharsets.UTF_8);
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
