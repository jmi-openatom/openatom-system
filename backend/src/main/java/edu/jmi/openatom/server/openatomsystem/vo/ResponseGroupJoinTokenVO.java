package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** QQ 群入群验证码响应。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseGroupJoinTokenVO {
  private String token;
  private Integer expiresIn;
}
