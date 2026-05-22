package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 机器人公开查人响应。
 *
 * <p>只返回机器人回复需要的低敏字段，不暴露手机号、邮箱、密码或 QQ 原始绑定值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBotUserLookupVO {
  private Integer id;
  private String userName;
  private String realName;
  private String studentId;
  private String college;
  private String major;
  private String grade;
  private String className;
  private Boolean qqBound;
  private String userStatus;
}
