package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;

public interface ApplicationService {
  ApiResponse<PageDataDTO<ResponseApplicationDTO>> list(
      Integer campaignId,
      Integer clubId,
      String status,
      Integer departmentId,
      String keyword,
      Long page,
      Long pageSize);

  ApiResponse<Integer> create(RequestCreateApplicationDTO request);

  ApiResponse<MembershipApplication> detail(Integer applicationId);

  ApiResponse<String> update(Integer applicationId, RequestUpdateApplicationDTO request);

  ApiResponse<String> submit(Integer applicationId);

  ApiResponse<String> withdraw(Integer applicationId);
}
