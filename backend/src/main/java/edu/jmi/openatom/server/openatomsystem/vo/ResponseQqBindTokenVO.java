package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** QQ 绑定码响应。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseQqBindTokenVO {
  private String token;
  private Integer expiresIn;
}
