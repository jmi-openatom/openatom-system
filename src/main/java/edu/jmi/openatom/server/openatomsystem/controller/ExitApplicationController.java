package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateExitApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestForceExitDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestRejectExitApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ExitApplication;
import edu.jmi.openatom.server.openatomsystem.service.ExitApplicationService;
import edu.jmi.openatom.server.openatomsystem.service.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExitApplicationController {
  private final ExitApplicationService exitApplicationService;
  private final MembershipService membershipService;

  @GetMapping("/exit-applications")
  @SaCheckPermission("exit-application:list")
  public ApiResponse<List<ExitApplication>> list() {
    return exitApplicationService.list();
  }

  @PostMapping("/exit-applications")
  @SaCheckPermission("exit-application:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateExitApplicationDTO request) {
    return exitApplicationService.create(request);
  }

  @GetMapping("/exit-applications/{exitApplicationId}")
  @SaCheckPermission("exit-application:detail")
  public ApiResponse<ExitApplication> detail(@PathVariable Integer exitApplicationId) {
    return exitApplicationService.detail(exitApplicationId);
  }

  @PostMapping("/exit-applications/{exitApplicationId}/approve")
  @SaCheckPermission("exit-application:approve")
  public ApiResponse<String> approve(@PathVariable Integer exitApplicationId) {
    return exitApplicationService.approve(exitApplicationId);
  }

  @PostMapping("/exit-applications/{exitApplicationId}/reject")
  @SaCheckPermission("exit-application:reject")
  public ApiResponse<String> reject(
      @PathVariable Integer exitApplicationId,
      @RequestBody(required = false) RequestRejectExitApplicationDTO request) {
    return exitApplicationService.reject(
        exitApplicationId, request == null ? null : request.getComment());
  }

  @PostMapping("/memberships/{membershipId}/force-exit")
  @SaCheckPermission("membership:force-exit")
  public ApiResponse<String> forceExit(
      @PathVariable Integer membershipId, @RequestBody(required = false) RequestForceExitDTO request) {
    return membershipService.forceExit(membershipId, request == null ? null : request.getReason());
  }
}
