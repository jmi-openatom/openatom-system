package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNewRoleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRoleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import java.util.List;

public interface RoleService {
  ApiResponse<List<Role>> getRoles();

  ApiResponse<String> createNewRole(RequestCreateNewRoleDTO requestCreateNewRoleDTO);

  ApiResponse<Role> getRoleByRoleId(Integer roleId);

  ApiResponse<String> updateRole(Integer roleId, RequestUpdateRoleDTO requestUpdateRoleDTO);

  ApiResponse<String> deleteRole(Integer roleId);

  ApiResponse<String> assignRolePermissions(Integer roleId, List<Integer> permissionIds);

  ApiResponse<String> assignUserRoles(Integer userId, List<Integer> roleIds);
}
