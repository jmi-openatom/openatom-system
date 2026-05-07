package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import edu.jmi.openatom.server.openatomsystem.service.SiteFormService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SiteFormController {
  private final SiteFormService siteFormService;

  @GetMapping("/clubs/{clubId}/site-forms")
  @SaCheckPermission("site-form:list")
  public ApiResponse<List<SiteForm>> list(@PathVariable Integer clubId) {
    return siteFormService.listByClub(clubId);
  }

  @PostMapping("/clubs/{clubId}/site-forms")
  @SaCheckPermission("site-form:create")
  public ApiResponse<String> create(
      @PathVariable Integer clubId, @Valid @RequestBody RequestCreateSiteFormDTO request) {
    return siteFormService.create(clubId, request);
  }

  @GetMapping("/site-forms/{formId}")
  @SaCheckPermission("site-form:detail")
  public ApiResponse<SiteForm> detail(@PathVariable Integer formId) {
    return siteFormService.detail(formId);
  }

  @PatchMapping("/site-forms/{formId}")
  @SaCheckPermission("site-form:update")
  public ApiResponse<String> update(
      @PathVariable Integer formId, @RequestBody RequestUpdateSiteFormDTO request) {
    return siteFormService.update(formId, request);
  }

  @PostMapping("/site-forms/{formId}/publish")
  @SaCheckPermission("site-form:update")
  public ApiResponse<String> publish(@PathVariable Integer formId) {
    return siteFormService.publish(formId);
  }

  @PostMapping("/site-forms/{formId}/close")
  @SaCheckPermission("site-form:update")
  public ApiResponse<String> close(@PathVariable Integer formId) {
    return siteFormService.close(formId);
  }
}
