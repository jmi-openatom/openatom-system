package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestCreateActivityDTO {
  @NotBlank(message = "活动标题不能为空")
  private String title;

  private String summary;
  private String descriptionMarkdown;
  private String activityAt;
  private String endAt;
  private String location;
  private String status;
  private String coverUrl;
  private Boolean registrationRequired;
  private String registrationStartAt;
  private String registrationEndAt;
  private Object registrationFields;
}
