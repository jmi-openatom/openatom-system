package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationController {
  private final ApplicationService applicationService;

  @GetMapping("/applications")
  @SaCheckPermission("application:list")
  public ApiResponse<PageDataDTO<ResponseApplicationDTO>> list(
      @RequestParam(required = false) Integer campaignId,
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer departmentId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") Long page,
      @RequestParam(defaultValue = "10") Long pageSize) {
    return applicationService.list(
        campaignId, clubId, status, departmentId, keyword, page, pageSize);
  }

  @PostMapping("/applications")
  public ApiResponse<Integer> create(@Valid @RequestBody RequestCreateApplicationDTO request) {
    return applicationService.create(request);
  }

  @GetMapping("/applications/{applicationId}")
  @SaCheckPermission("application:detail")
  public ApiResponse<MembershipApplication> detail(@PathVariable Integer applicationId) {
    return applicationService.detail(applicationId);
  }

  @PatchMapping("/applications/{applicationId}")
  @SaCheckPermission("application:update")
  public ApiResponse<String> update(
      @PathVariable Integer applicationId,
      @Valid @RequestBody RequestUpdateApplicationDTO request) {
    return applicationService.update(applicationId, request);
  }

  @PostMapping("/applications/{applicationId}/submit")
  @SaCheckPermission("application:submit")
  public ApiResponse<String> submit(@PathVariable Integer applicationId) {
    return applicationService.submit(applicationId);
  }

  @PostMapping("/applications/{applicationId}/withdraw")
  @SaCheckPermission("application:withdraw")
  public ApiResponse<String> withdraw(@PathVariable Integer applicationId) {
    return applicationService.withdraw(applicationId);
  }
}
