package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.Builder;
import lombok.Data;

/** 积分规则配置 */
@Data
@Builder
public class ResponsePointRuleSettingsVO {
  private Integer dailyLoginPoints;
  private Integer blogPublishPoints;
}
