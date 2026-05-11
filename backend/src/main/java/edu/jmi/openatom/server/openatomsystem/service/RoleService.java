package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNewRoleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRoleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import java.util.List;

/**
 * 角色管理服务接口
 *
 * <p>定义角色列表查询, 创建, 查看详情, 更新, 删除, 分配角色权限, 分配用户角色以及查询用户角色等业务操作
 */
public interface RoleService {
  ApiResponse<List<Role>> getRoles();

  ApiResponse<String> createNewRole(RequestCreateNewRoleDTO requestCreateNewRoleDTO);

  ApiResponse<Role> getRoleByRoleId(Integer roleId);

  ApiResponse<String> updateRole(Integer roleId, RequestUpdateRoleDTO requestUpdateRoleDTO);

  ApiResponse<String> deleteRole(Integer roleId);

  ApiResponse<String> assignRolePermissions(Integer roleId, List<Integer> permissionIds);

  ApiResponse<String> assignUserRoles(Integer userId, List<Integer> roleIds);

  ApiResponse<List<Role>> getUserRoles(Integer userId);
}
