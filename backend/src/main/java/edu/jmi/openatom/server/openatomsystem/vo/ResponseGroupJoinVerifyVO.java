package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** QQ 群入群验证码校验结果——供机器人调用，返回用户名片信息。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseGroupJoinVerifyVO {
  private Integer userId;
  private String realName;
  private String studentId;
  private String departmentName;
  private String cardName;
  private String qqOpenid;
}
