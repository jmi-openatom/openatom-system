package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批动作请求
 *
 * <p>用于提交审批操作, 包含审批动作action, 审批节点node, 审批意见comment以及下一轮面试官ID列表nextInterviewerIds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestApprovalActionDTO {
  @NotBlank(message = "审批动作不能为空")
  private String action;

  @NotBlank(message = "审批节点不能为空")
  private String node;

  private String comment;
  private List<Integer> nextInterviewerIds;
}
