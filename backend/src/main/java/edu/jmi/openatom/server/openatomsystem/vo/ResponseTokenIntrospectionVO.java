package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTokenIntrospectionVO {
  private Boolean active;
  private String sub;
  private String username;
  private String name;
  private String clientId;
  private String scope;
  private Long exp;
  private Long expiresIn;
  private List<String> roles;
  private List<String> permissions;
}
