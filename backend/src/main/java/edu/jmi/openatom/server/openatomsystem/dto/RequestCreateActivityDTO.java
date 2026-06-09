package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建活动请求
 *
 * <p>用于创建新活动, 包含活动标题title, 摘要summary, 描述descriptionMarkdown, 活动时间activityAt和endAt, 地点location, 状态status, 封面coverUrl, 报名设置registrationRequired, 报名时间范围registrationStartAt和registrationEndAt以及自定义报名字段registrationFields
 */
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
  private Long participationPoints;
}
