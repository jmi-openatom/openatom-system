package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestSaveAiActivityPlanDTO {
  @NotBlank(message = "策划案内容不能为空")
  private String contentMarkdown;
}
