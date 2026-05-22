package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAssignUserRolesDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestResetPasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateUserStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUserUpdateDTO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseAvatarHealthVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMembershipVO;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.service.RoleService;
import edu.jmi.openatom.server.openatomsystem.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 用户管理控制器
 *
 * <p>提供用户信息的分页查询, 创建, 导入, 角色分配, 状态变更, 密码重置及成员资格查询等功能
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
  private final RoleService roleService;
  private final UserService userService;

  /**
   * 分页查询用户列表
   *
   * @param keyword 搜索关键词（可选）
   * @param status 用户状态（可选）
   * @param clubId 社团ID（可选）
   * @param qqOpenid QQ号/QQ OpenID（可选）
   * @param page 页码，默认 1
   * @param pageSize 每页条数，默认 10
   * @return 分页用户数据
   */
  @GetMapping
  @SaCheckPermission("user:list")
  public Result<PageDataVO<User>> users(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String qqOpenid,
      @RequestParam(defaultValue = "1") Long page,
      @RequestParam(defaultValue = "10") Long pageSize) {
    return userService.getUsers(
        keyword,
        status == null || status.isBlank() ? null : UserStatus.fromValue(status),
        clubId,
        qqOpenid,
        page,
        pageSize);
  }

  /**
   * 创建新用户
   *
   * @param requestCreateUserDTO 创建用户请求参数
   * @return 创建结果
   */
  @PostMapping
  @SaCheckPermission("user:create")
  public Result<String> createUser(
      @Valid @RequestBody RequestCreateUserDTO requestCreateUserDTO) {
    return userService.createUser(requestCreateUserDTO);
  }

  /**
   * 批量导入用户
   *
   * @param file 用户导入文件
   * @return 导入结果
   */
  @PostMapping("/import")
  @SaCheckPermission("user:import")
  public Result<String> importUsers(@RequestParam("file") MultipartFile file) {
    return userService.importUsers(file);
  }

  /**
   * 下载用户导入模板
   *
   * @return 模板文件字节流
   */
  @GetMapping("/import/template")
  @SaCheckPermission("user:import")
  public ResponseEntity<byte[]> downloadTemplate() {
    byte[] bytes = userService.exportTemplate();
    String fileName = URLEncoder.encode("user-import-template.xlsx", StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  /**
   * 为用户分配角色
   *
   * @param userId 用户ID
   * @param requestAssignUserRolesDTO 分配角色请求参数
   * @return 分配结果
   */
  @PostMapping("/{userId}/roles")
  @SaCheckPermission("user:role:assign")
  public Result<String> assignUserRoles(
      @PathVariable Integer userId,
      @Valid @RequestBody RequestAssignUserRolesDTO requestAssignUserRolesDTO) {
    return roleService.assignUserRoles(
        userId, requestAssignUserRolesDTO == null ? null : requestAssignUserRolesDTO.getRoleIds());
  }

  /**
   * 获取用户已分配的角色
   *
   * @param userId 用户ID
   * @return 角色列表
   */
  @GetMapping("/{userId}/roles")
  @SaCheckPermission("user:role:assign")
  public Result<List<Role>> getUserRoles(@PathVariable Integer userId) {
    return roleService.getUserRoles(userId);
  }

  /**
   * 获取用户详情
   *
   * @param userId 用户ID
   * @return 用户详情
   */
  @GetMapping("/{userId}")
  @SaCheckPermission("user:info")
  public Result<User> info(@PathVariable Integer userId) {
    return userService.infoByUserId(userId);
  }

  /**
   * 更新用户信息
   *
   * @param userId 用户ID
   * @param requestUserUpdate 更新用户请求参数
   * @return 更新结果
   */
  @PatchMapping("/{userId}")
  @SaCheckPermission("user:update")
  public Result<String> update(
      @PathVariable Integer userId, @Valid @RequestBody RequestUserUpdateDTO requestUserUpdate) {
    return userService.updateUserInfo(userId, requestUserUpdate);
  }

  /**
   * 删除用户
   *
   * @param userId 用户ID
   * @return 删除结果
   */
  @DeleteMapping("/{userId}")
  @SaCheckPermission("user:delete")
  public Result<String> delete(@PathVariable Integer userId) {
    return userService.deleteUser(userId);
  }

  /**
   * 更新用户状态
   *
   * @param userId 用户ID
   * @param requestUpdateUserStatusDTO 更新状态请求参数
   * @return 更新结果
   */
  @PatchMapping("/{userId}/status")
  @SaCheckPermission("user:status:update")
  public Result<String> updateStatus(
      @PathVariable Integer userId,
      @Valid @RequestBody RequestUpdateUserStatusDTO requestUpdateUserStatusDTO) {
    return userService.updateUserStatus(userId, requestUpdateUserStatusDTO);
  }

  /**
   * 重置用户密码
   *
   * @param userId 用户ID
   * @param requestResetPasswordDTO 重置密码请求参数
   * @return 重置结果
   */
  @PostMapping("/{userId}/reset-password")
  @SaCheckPermission("user:password:reset")
  public Result<String> resetPassword(
      @PathVariable Integer userId,
      @Valid @RequestBody RequestResetPasswordDTO requestResetPasswordDTO) {
    return userService.resetPassword(userId, requestResetPasswordDTO);
  }

  /**
   * 获取用户的社团成员资格列表
   *
   * @param userId 用户ID
   * @return 成员资格列表
   */
  @GetMapping("/{userId}/memberships")
  @SaCheckPermission("user:membership:list")
  public Result<List<ResponseMembershipVO>> memberships(@PathVariable Integer userId) {
    return userService.getUserMemberships(userId);
  }

  @GetMapping("/avatars/health")
  @SaCheckPermission("user:list")
  public Result<ResponseAvatarHealthVO> avatarHealth() {
    return userService.getAvatarHealth();
  }

  @PostMapping("/avatars/cleanup")
  @SaCheckPermission("user:update")
  public Result<Integer> cleanupInvalidAvatars() {
    return userService.cleanupInvalidAvatars();
  }
}
