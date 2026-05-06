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
