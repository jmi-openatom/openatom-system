package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户信息DTO
 *
 * <p>包含当前登录用户的详细信息, 所属角色列表及权限列表, 用于前端权限校验和用户状态展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseCurrentUserDTO {
  private User user;
  private List<String> roles;
  private List<String> permissions;
}
