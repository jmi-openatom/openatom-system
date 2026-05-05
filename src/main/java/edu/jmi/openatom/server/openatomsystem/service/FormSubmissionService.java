package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateFormSubmissionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseFormSubmissionDTO;

public interface FormSubmissionService {
  ApiResponse<Integer> create(Integer campaignId, RequestCreateFormSubmissionDTO request);

  ApiResponse<PageDataDTO<ResponseFormSubmissionDTO>> list(
      Integer campaignId, String keyword, Long page, Long pageSize);

  byte[] exportExcel(Integer campaignId);
}
