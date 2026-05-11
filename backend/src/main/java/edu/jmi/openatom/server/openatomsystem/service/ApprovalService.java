package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestApprovalActionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBatchApprovalDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import java.util.List;

/**
 * 审批服务接口
 *
 * <p>定义审批记录的查询, 单个审批和批量审批等业务操作
 */
public interface ApprovalService {
  Result<List<ApprovalRecord>> records(Integer applicationId);

  Result<String> approve(Integer applicationId, RequestApprovalActionDTO request);

  Result<String> batchApprove(RequestBatchApprovalDTO request);
}
