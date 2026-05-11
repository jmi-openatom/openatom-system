package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 *
 * <p>包含登录成功后的访问令牌, 刷新令牌, 过期时间, 当前用户信息及权限角色数据
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseLoginDTO {
  private String accessToken;
  private String refreshToken;
  private Integer expiresIn;
  private User user;
  private List<String> roles;
  private List<String> permissions;
}
