package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestApprovalActionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestBatchApprovalDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.mapper.ApprovalRecordMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.service.ApprovalService;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审批处理实现类
 *
 * <p>负责入会申请的审批, 批量审批, 审批记录查询以及审批通过后发送通知等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {
  private static final List<String> ACTIONS = List.of("approve", "reject", "transfer", "request_more_info");

  private final ApprovalRecordMapper approvalRecordMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final NotificationService notificationService;

  @Override
  public ApiResponse<List<ApprovalRecord>> records(Integer applicationId) {
    if (applicationId == null || applicationMapper.selectById(applicationId) == null)
      return ApiResponse.error(404, "申请不存在");
    return ApiResponse.success(approvalRecordMapper.selectByApplicationIdOrdered(applicationId));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> approve(Integer applicationId, RequestApprovalActionDTO request) {
    MembershipApplication application = applicationId == null ? null : applicationMapper.selectById(applicationId);
    if (application == null) return ApiResponse.error(404, "申请不存在");
    if (!ACTIONS.contains(request.getAction())) return ApiResponse.error(400, "审批动作不合法");
    approvalRecordMapper.insert(ApprovalRecord.builder().applicationId(applicationId)
        .node(request.getNode()).action(request.getAction()).operatorId(StpUtil.getLoginIdAsInt())
        .comment(request.getComment()).build());
    application.setStatus(resolveNextStatus(application.getStatus(), request));
    applicationMapper.updateById(application);
    String title = "申请进度更新";
    String content = String.format("您的入会申请有了新的动态：当前状态为【%s】。", application.getStatus());
    if (request.getComment() != null && !request.getComment().isBlank()) content += "\n审核意见：" + request.getComment();
    notificationService.create(RequestCreateNotificationDTO.builder().title(title).content(content)
        .type("approval").receiverUserIds(List.of(application.getUserId())).build());
    return ApiResponse.success("审批处理成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> batchApprove(RequestBatchApprovalDTO request) {
    if (request.getApproval() == null) return ApiResponse.error(400, "审批参数不能为空");
    for (Integer applicationId : request.getApplicationIds()) {
      ApiResponse<String> response = approve(applicationId, request.getApproval());
      if (response.getCode() != ApiResponse.SUCCESS_CODE) return response;
    }
    return ApiResponse.success("批量审批成功");
  }

  private String resolveNextStatus(String currentStatus, RequestApprovalActionDTO request) {
    if ("reject".equals(request.getAction())) return "pre_screen".equals(request.getNode()) ? "pre_screen_rejected" : "rejected";
    if ("request_more_info".equals(request.getAction())) return "submitted";
    if ("transfer".equals(request.getAction())) return currentStatus;
    if ("pre_screen".equals(request.getNode())) return "pre_screen_passed";
    return currentStatus;
  }
}
