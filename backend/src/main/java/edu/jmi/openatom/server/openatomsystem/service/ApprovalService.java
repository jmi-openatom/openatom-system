package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestApprovalActionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestBatchApprovalDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import java.util.List;

/**
 * 审批服务接口
 *
 * <p>定义审批记录的查询, 单个审批和批量审批等业务操作
 */
public interface ApprovalService {
  ApiResponse<List<ApprovalRecord>> records(Integer applicationId);

  ApiResponse<String> approve(Integer applicationId, RequestApprovalActionDTO request);

  ApiResponse<String> batchApprove(RequestBatchApprovalDTO request);
}
