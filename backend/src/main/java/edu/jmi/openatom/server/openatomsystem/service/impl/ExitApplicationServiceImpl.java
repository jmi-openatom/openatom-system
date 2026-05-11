package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateExitApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import edu.jmi.openatom.server.openatomsystem.entity.ExitApplication;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ExitApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.service.ExitApplicationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 退社申请管理实现类
 *
 * <p>负责退社申请的创建, 查询, 审批通过和驳回等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ExitApplicationServiceImpl implements ExitApplicationService {
  private final ExitApplicationMapper exitApplicationMapper;
  private final ClubMembershipMapper membershipMapper;

  @Override
  public ApiResponse<List<ExitApplication>> list() {
    return ApiResponse.success(exitApplicationMapper.selectAllOrdered());
  }

  @Override
  public ApiResponse<String> create(RequestCreateExitApplicationDTO request) {
    ClubMembership membership = membershipMapper.selectById(request.getMembershipId());
    if (membership == null) return ApiResponse.error(404, "成员不存在");
    if ("left".equals(membership.getStatus())) return ApiResponse.error(422, "成员已退社");
    exitApplicationMapper.insert(ExitApplication.builder().membershipId(request.getMembershipId())
        .reason(request.getReason()).description(request.getDescription()).status("submitted").build());
    return ApiResponse.success("退社申请提交成功");
  }

  @Override
  public ApiResponse<ExitApplication> detail(Integer exitApplicationId) {
    ExitApplication application = findExitApplication(exitApplicationId);
    return application == null ? ApiResponse.error(404, "退社申请不存在") : ApiResponse.success(application);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> approve(Integer exitApplicationId) {
    ExitApplication application = findExitApplication(exitApplicationId);
    if (application == null) return ApiResponse.error(404, "退社申请不存在");
    ClubMembership membership = membershipMapper.selectById(application.getMembershipId());
    if (membership != null) { membership.setStatus("left"); membership.setLeftAt(Times.now()); membershipMapper.updateById(membership); }
    application.setStatus("approved");
    exitApplicationMapper.updateById(application);
    return ApiResponse.success("退社申请已通过");
  }

  @Override
  public ApiResponse<String> reject(Integer exitApplicationId, String comment) {
    ExitApplication application = findExitApplication(exitApplicationId);
    if (application == null) return ApiResponse.error(404, "退社申请不存在");
    application.setStatus("rejected");
    exitApplicationMapper.updateById(application);
    return ApiResponse.success("退社申请已驳回");
  }

  private ExitApplication findExitApplication(Integer exitApplicationId) {
    return exitApplicationId == null ? null : exitApplicationMapper.selectById(exitApplicationId);
  }
}
