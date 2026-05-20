package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** QQ 绑定码确认请求。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestConfirmQqBindDTO {
  @NotBlank(message = "绑定码不能为空")
  private String token;

  @NotBlank(message = "QQ号不能为空")
  private String qqOpenid;
}
