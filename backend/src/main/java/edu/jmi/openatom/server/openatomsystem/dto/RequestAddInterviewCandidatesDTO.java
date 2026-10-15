package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Data;

@Data
public class RequestAddInterviewCandidatesDTO {
  @NotEmpty(message = "请选择候选人")
  private List<@NotNull @Positive Integer> applicationIds;
  /** 留空时在本场次的面试间中自动分配。 */
  private Integer roomId;
  private Boolean previewOnly = true;
}
