package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestApprovalActionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBatchApprovalDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import edu.jmi.openatom.server.openatomsystem.service.ApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批控制器
 *
 * <p>提供审批记录的查询及审批操作
 */
@RestController
@RequiredArgsConstructor
public class ApprovalController {
  private final ApprovalService approvalService;

  /**
   * 获取申请表审批记录
   *
   * @param applicationId 申请表ID
   * @return 审批记录列表
   */
  @GetMapping("/applications/{applicationId}/approval-records")
  @SaCheckPermission("approval-record:list")
  public Result<List<ApprovalRecord>> records(@PathVariable Integer applicationId) {
    return approvalService.records(applicationId);
  }

  /**
   * 审批申请表
   *
   * @param applicationId 申请表ID
   * @param request 审批操作请求
   * @return 操作结果
   */
  @PostMapping("/applications/{applicationId}/approvals")
  @SaCheckPermission("application:approve")
  public Result<String> approve(
      @PathVariable Integer applicationId, @Valid @RequestBody RequestApprovalActionDTO request) {
    return approvalService.approve(applicationId, request);
  }

  /**
   * 批量审批申请表
   *
   * @param request 批量审批请求
   * @return 操作结果
   */
  @PostMapping("/applications/batch-approvals")
  @SaCheckPermission("application:batch-approve")
  public Result<String> batchApprove(@Valid @RequestBody RequestBatchApprovalDTO request) {
    return approvalService.batchApprove(request);
  }
}
