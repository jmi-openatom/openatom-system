package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.Data;

/** 积分规则配置请求 */
@Data
public class RequestPointRuleSettingsDTO {
  private Integer dailyLoginPoints;
  private Integer blogPublishPoints;
}
