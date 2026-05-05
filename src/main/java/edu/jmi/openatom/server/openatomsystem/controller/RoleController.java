package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestAssignRolePermissionsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNewRoleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRoleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roles")
public class RoleController {
  private final RoleService roleService;

  @GetMapping
  @SaCheckPermission("role:list")
  public ApiResponse<List<Role>> roles() {
    return roleService.getRoles();
  }

  @PostMapping
  @SaCheckPermission("role:create")
  public ApiResponse<String> createNewRole(
      @Valid @RequestBody RequestCreateNewRoleDTO requestCreateNewRoleDTO) {
    return roleService.createNewRole(requestCreateNewRoleDTO);
  }

  @GetMapping("/{roleId}")
  @SaCheckPermission("role:detail")
  public ApiResponse<Role> getRoleByRoleId(@PathVariable Integer roleId) {
    return roleService.getRoleByRoleId(roleId);
  }

  @PatchMapping("/{roleId}")
  @SaCheckPermission("role:update")
  public ApiResponse<String> updateRole(
      @PathVariable Integer roleId, @Valid @RequestBody RequestUpdateRoleDTO requestUpdateRoleDTO) {
    return roleService.updateRole(roleId, requestUpdateRoleDTO);
  }

  @DeleteMapping("/{roleId}")
  @SaCheckPermission("role:delete")
  public ApiResponse<String> deleteRole(@PathVariable Integer roleId) {
    return roleService.deleteRole(roleId);
  }

  @PostMapping("/{roleId}/permissions")
  @SaCheckPermission("role:permission:assign")
  public ApiResponse<String> assignRolePermissions(
      @PathVariable Integer roleId,
      @Valid @RequestBody RequestAssignRolePermissionsDTO requestAssignRolePermissionsDTO) {
    return roleService.assignRolePermissions(
        roleId,
        requestAssignRolePermissionsDTO == null
            ? null
            : requestAssignRolePermissionsDTO.getPermissionIds());
  }
}
