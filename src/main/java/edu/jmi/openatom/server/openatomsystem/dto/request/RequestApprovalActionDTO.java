package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
