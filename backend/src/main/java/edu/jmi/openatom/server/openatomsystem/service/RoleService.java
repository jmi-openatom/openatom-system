package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNewRoleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRoleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import java.util.List;

/**
 * 角色管理服务接口
 *
 * <p>定义角色列表查询, 创建, 查看详情, 更新, 删除, 分配角色权限, 分配用户角色以及查询用户角色等业务操作
 */
public interface RoleService {
  Result<List<Role>> getRoles();

  Result<String> createNewRole(RequestCreateNewRoleDTO requestCreateNewRoleDTO);

  Result<Role> getRoleByRoleId(Integer roleId);

  Result<String> updateRole(Integer roleId, RequestUpdateRoleDTO requestUpdateRoleDTO);

  Result<String> deleteRole(Integer roleId);

  Result<String> assignRolePermissions(Integer roleId, List<Integer> permissionIds);

  Result<String> assignUserRoles(Integer userId, List<Integer> roleIds);

  Result<List<Role>> getUserRoles(Integer userId);
}
