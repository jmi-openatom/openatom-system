package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestCreateAwardDTO {
  @NotBlank(message = "获奖标题不能为空")
  private String title;

  @NotBlank(message = "比赛名称不能为空")
  private String competitionName;

  private String awardLevel;
  private Integer awardYear;
  private String teamName;
  private String description;
  private Integer sortOrder;
}
