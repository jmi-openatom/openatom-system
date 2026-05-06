package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateFormSubmissionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseFormSubmissionDTO;
import edu.jmi.openatom.server.openatomsystem.service.FormSubmissionService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FormSubmissionController {
  private final FormSubmissionService formSubmissionService;

  @PostMapping("/site/forms/{formId}/submissions")
  public ApiResponse<Integer> create(
      @PathVariable Integer formId,
      @Valid @RequestBody RequestCreateFormSubmissionDTO request) {
    return formSubmissionService.create(formId, request);
  }

  @GetMapping("/site-forms/{formId}/submissions")
  @SaCheckPermission("site-form:detail")
  public ApiResponse<PageDataDTO<ResponseFormSubmissionDTO>> list(
      @PathVariable Integer formId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") Long page,
      @RequestParam(defaultValue = "10") Long pageSize) {
    return formSubmissionService.list(formId, keyword, page, pageSize);
  }

  @GetMapping("/site-forms/{formId}/submissions/export")
  @SaCheckPermission("site-form:detail")
  public ResponseEntity<byte[]> export(@PathVariable Integer formId) {
    byte[] bytes = formSubmissionService.exportExcel(formId);
    String fileName =
        URLEncoder.encode("form-submissions-" + formId + ".xlsx", StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }
}
