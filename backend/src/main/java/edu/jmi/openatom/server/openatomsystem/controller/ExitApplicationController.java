package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateExitApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestForceExitDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRejectExitApplicationDTO;
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

/**
 * 退社申请控制器
 *
 * <p>提供退社申请的增删改查, 审批与强制退社等操作
 */
@RestController
@RequiredArgsConstructor
public class ExitApplicationController {
  private final ExitApplicationService exitApplicationService;
  private final MembershipService membershipService;

  /**
   * 获取退社申请列表
   *
   * @return 退社申请列表
   */
  @GetMapping("/exit-applications")
  @SaCheckPermission("exit-application:list")
  public Result<List<ExitApplication>> list() {
    return exitApplicationService.list();
  }

  /**
   * 创建退社申请
   *
   * @param request 创建退社申请请求
   * @return 操作结果
   */
  @PostMapping("/exit-applications")
  @SaCheckPermission("exit-application:create")
  public Result<String> create(@Valid @RequestBody RequestCreateExitApplicationDTO request) {
    return exitApplicationService.create(request);
  }

  /**
   * 获取退社申请详情
   *
   * @param exitApplicationId 退社申请ID
   * @return 退社申请详情
   */
  @GetMapping("/exit-applications/{exitApplicationId}")
  @SaCheckPermission("exit-application:detail")
  public Result<ExitApplication> detail(@PathVariable Integer exitApplicationId) {
    return exitApplicationService.detail(exitApplicationId);
  }

  /**
   * 审批通过退社申请
   *
   * @param exitApplicationId 退社申请ID
   * @return 操作结果
   */
  @PostMapping("/exit-applications/{exitApplicationId}/approve")
  @SaCheckPermission("exit-application:approve")
  public Result<String> approve(@PathVariable Integer exitApplicationId) {
    return exitApplicationService.approve(exitApplicationId);
  }

  /**
   * 驳回退社申请
   *
   * @param exitApplicationId 退社申请ID
   * @param request 驳回理由
   * @return 操作结果
   */
  @PostMapping("/exit-applications/{exitApplicationId}/reject")
  @SaCheckPermission("exit-application:reject")
  public Result<String> reject(
      @PathVariable Integer exitApplicationId,
      @RequestBody(required = false) RequestRejectExitApplicationDTO request) {
    return exitApplicationService.reject(
        exitApplicationId, request == null ? null : request.getComment());
  }

  /**
   * 强制退社
   *
   * @param membershipId 成员ID
   * @param request 强制退社请求
   * @return 操作结果
   */
  @PostMapping("/memberships/{membershipId}/force-exit")
  @SaCheckPermission("membership:force-exit")
  public Result<String> forceExit(
      @PathVariable Integer membershipId,
      @RequestBody(required = false) RequestForceExitDTO request) {
    return membershipService.forceExit(membershipId, request == null ? null : request.getReason());
  }
}
