package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 请假审批请求 */
@Data
public class RequestReviewLeaveApplicationDTO {
  @NotBlank(message = "审批动作不能为空")
  private String action;

  private String comment;
}
