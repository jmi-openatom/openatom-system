package edu.jmi.openatom.server.openatomsystem.vo;

import edu.jmi.openatom.server.openatomsystem.entity.Role;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色列表VO
 *
 * <p>封装系统中的角色数据列表, 用于角色管理与权限分配
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseRolesVO {
  List<Role> roles;
}
