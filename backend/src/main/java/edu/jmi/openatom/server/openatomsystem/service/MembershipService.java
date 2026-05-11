package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestAssignPositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestChangeMembershipStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestFinalDecisionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import java.util.List;

/**
 * 社员管理服务接口
 *
 * <p>定义社员的最终录取决定, 列表查询, 创建, 查看详情, 更新, 分配职位, 变更状态和强制退社等业务操作
 */
public interface MembershipService {
  ApiResponse<String> finalDecision(Integer applicationId, RequestFinalDecisionDTO request);

  ApiResponse<List<ResponseMembershipDTO>> list(
      Integer clubId, Integer departmentId, Integer positionId, String status, String keyword);

  ApiResponse<String> create(RequestCreateMembershipDTO request);

  ApiResponse<ClubMembership> detail(Integer membershipId);

  ApiResponse<String> update(Integer membershipId, RequestUpdateMembershipDTO request);

  ApiResponse<String> assignPosition(Integer membershipId, RequestAssignPositionDTO request);

  ApiResponse<String> changeStatus(Integer membershipId, RequestChangeMembershipStatusDTO request);

  ApiResponse<String> forceExit(Integer membershipId, String reason);
}
