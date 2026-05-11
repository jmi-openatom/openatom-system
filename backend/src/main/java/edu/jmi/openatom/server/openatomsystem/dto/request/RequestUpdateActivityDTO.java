package edu.jmi.openatom.server.openatomsystem.dto.request;

import lombok.Data;

/**
 * 更新活动请求
 *
 * <p>用于更新活动信息, 包含活动标题title, 摘要summary, 描述descriptionMarkdown, 活动时间activityAt和endAt, 地点location, 状态status, 封面coverUrl, 报名设置registrationRequired, 报名时间范围registrationStartAt和registrationEndAt以及自定义报名字段registrationFields
 */
@Data
public class RequestUpdateActivityDTO {
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
