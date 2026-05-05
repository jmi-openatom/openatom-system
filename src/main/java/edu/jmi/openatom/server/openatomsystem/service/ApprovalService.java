package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestApprovalActionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestBatchApprovalDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import java.util.List;

public interface ApprovalService {
  ApiResponse<List<ApprovalRecord>> records(Integer applicationId);

  ApiResponse<String> approve(Integer applicationId, RequestApprovalActionDTO request);

  ApiResponse<String> batchApprove(RequestBatchApprovalDTO request);
}
