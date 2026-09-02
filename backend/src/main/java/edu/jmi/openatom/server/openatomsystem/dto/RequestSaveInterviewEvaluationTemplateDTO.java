package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class RequestSaveInterviewEvaluationTemplateDTO {
  @NotNull private Integer campaignId;
  @NotBlank private String name;
  @NotNull private Map<String, Object> schema;
}
