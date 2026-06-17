package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAssignRolePermissionsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNewRoleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRoleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理控制器
 *
 * <p>提供系统角色的查询, 创建, 详情, 更新, 删除及权限分配等功能
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/roles")
public class RoleController {
  private final RoleService roleService;

  /**
   * 获取所有角色列表
   *
   * @return 角色列表
   */
  @GetMapping
  @SaCheckPermission("role:list")
  public Result<List<Role>> roles() {
    return roleService.getRoles();
  }

  /**
   * 创建新角色
   *
   * @param requestCreateNewRoleDTO 创建角色请求参数
   * @return 创建结果
   */
  @PostMapping
  @SaCheckPermission("role:create")
  public Result<String> createNewRole(
      @Valid @RequestBody RequestCreateNewRoleDTO requestCreateNewRoleDTO) {
    return roleService.createNewRole(requestCreateNewRoleDTO);
  }

  /**
   * 根据角色 ID 获取角色详情
   *
   * @param roleId 角色ID
   * @return 角色详情
   */
  @GetMapping("/{roleId}")
  @SaCheckPermission("role:detail")
  public Result<Map<String, Object>> getRoleByRoleId(@PathVariable Integer roleId) {
    return roleService.getRoleByRoleId(roleId);
  }

  /**
   * 更新角色信息
   *
   * @param roleId 角色ID
   * @param requestUpdateRoleDTO 更新角色请求参数
   * @return 更新结果
   */
  @PatchMapping("/{roleId}")
  @SaCheckPermission("role:update")
  public Result<String> updateRole(
      @PathVariable Integer roleId, @Valid @RequestBody RequestUpdateRoleDTO requestUpdateRoleDTO) {
    return roleService.updateRole(roleId, requestUpdateRoleDTO);
  }

  /**
   * 删除角色
   *
   * @param roleId 角色ID
   * @return 删除结果
   */
  @DeleteMapping("/{roleId}")
  @SaCheckPermission("role:delete")
  public Result<String> deleteRole(@PathVariable Integer roleId) {
    return roleService.deleteRole(roleId);
  }

  /**
   * 为角色分配权限
   *
   * @param roleId 角色ID
   * @param requestAssignRolePermissionsDTO 分配权限请求参数
   * @return 分配结果
   */
  @PostMapping("/{roleId}/permissions")
  @SaCheckPermission("role:permission:assign")
  public Result<String> assignRolePermissions(
      @PathVariable Integer roleId,
      @Valid @RequestBody RequestAssignRolePermissionsDTO requestAssignRolePermissionsDTO) {
    return roleService.assignRolePermissions(
        roleId,
        requestAssignRolePermissionsDTO == null
            ? null
            : requestAssignRolePermissionsDTO.getPermissionIds());
  }
}
