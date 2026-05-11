package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量审批请求
 *
 * <p>用于批量处理审批申请, 包含申请ID列表applicationIds和审批动作approval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestBatchApprovalDTO {
  @NotEmpty(message = "applicationIds不能为空")
  private List<Integer> applicationIds;

  private RequestApprovalActionDTO approval;
}
