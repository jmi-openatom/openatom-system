package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestOauthClientDTO {
  @NotBlank(message = "clientId不能为空")
  private String clientId;

  private String clientSecret;

  @NotBlank(message = "应用名称不能为空")
  private String clientName;

  @NotBlank(message = "回调地址不能为空")
  private String redirectUris;

  private String scopes;
  private String grantTypes;
  private Boolean enabled;
}
