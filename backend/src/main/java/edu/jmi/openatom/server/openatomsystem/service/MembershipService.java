package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAssignPositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBatchChangeMembershipStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBatchCreateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestChangeMembershipStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestFinalDecisionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMembershipVO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import java.util.List;

/**
 * 社员管理服务接口
 *
 * <p>定义社员的最终录取决定, 列表查询, 创建, 查看详情, 更新, 分配职位, 变更状态和强制退社等业务操作
 */
public interface MembershipService {
  Result<String> finalDecision(Integer applicationId, RequestFinalDecisionDTO request);

  Result<List<ResponseMembershipVO>> list(
      Integer clubId, Integer departmentId, Integer positionId, String status, String keyword);

  Result<String> create(RequestCreateMembershipDTO request);

  Result<ClubMembership> detail(Integer membershipId);

  Result<String> update(Integer membershipId, RequestUpdateMembershipDTO request);

  Result<String> assignPosition(Integer membershipId, RequestAssignPositionDTO request);

  Result<String> changeStatus(Integer membershipId, RequestChangeMembershipStatusDTO request);

  Result<String> batchChangeStatus(RequestBatchChangeMembershipStatusDTO request);

  Result<String> batchCreate(RequestBatchCreateMembershipDTO request);

  Result<String> forceExit(Integer membershipId, String reason);
}
