package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestAssignUserRolesDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestResetPasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateUserStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUserUpdate;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseMembershipDTO;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
  private final RoleService roleService;
  private final UserService userService;

  @GetMapping
  @SaCheckPermission("user:list")
  public ApiResponse<PageDataDTO<User>> users(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer clubId,
      @RequestParam(defaultValue = "1") Long page,
      @RequestParam(defaultValue = "10") Long pageSize) {
    return userService.getUsers(
        keyword, status == null || status.isBlank() ? null : UserStatus.fromValue(status), clubId, page, pageSize);
  }

  @PostMapping
  @SaCheckPermission("user:create")
  public ApiResponse<String> createUser(@Valid @RequestBody RequestCreateUserDTO requestCreateUserDTO) {
    return userService.createUser(requestCreateUserDTO);
  }

  @PostMapping("/import")
  @SaCheckPermission("user:import")
  public ApiResponse<String> importUsers(@RequestParam("file") MultipartFile file) {
    return userService.importUsers(file);
  }

  @GetMapping("/import/template")
  @SaCheckPermission("user:import")
  public ResponseEntity<byte[]> downloadTemplate() {
    byte[] bytes = userService.exportTemplate();
    String fileName = URLEncoder.encode("user-import-template.xlsx", StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @PostMapping("/{userId}/roles")
  @SaCheckPermission("user:role:assign")
  public ApiResponse<String> assignUserRoles(
      @PathVariable Integer userId,
      @Valid @RequestBody RequestAssignUserRolesDTO requestAssignUserRolesDTO) {
    return roleService.assignUserRoles(
        userId, requestAssignUserRolesDTO == null ? null : requestAssignUserRolesDTO.getRoleIds());
  }

  @GetMapping("/{userId}/roles")
  @SaCheckPermission("user:role:list")
  public ApiResponse<List<Role>> getUserRoles(@PathVariable Integer userId) {
    return roleService.getUserRoles(userId);
  }

  @GetMapping("/{userId}")
  @SaCheckPermission("user:info")
  public ApiResponse<User> info(@PathVariable Integer userId) {
    return userService.infoByUserId(userId);
  }

  @PatchMapping("/{userId}")
  @SaCheckPermission("user:update")
  public ApiResponse<String> update(
      @PathVariable Integer userId, @Valid @RequestBody RequestUserUpdate requestUserUpdate) {
    return userService.updateUserInfo(userId, requestUserUpdate);
  }

  @PatchMapping("/{userId}/status")
  @SaCheckPermission("user:status:update")
  public ApiResponse<String> updateStatus(
      @PathVariable Integer userId,
      @Valid @RequestBody RequestUpdateUserStatusDTO requestUpdateUserStatusDTO) {
    return userService.updateUserStatus(userId, requestUpdateUserStatusDTO);
  }

  @PostMapping("/{userId}/reset-password")
  @SaCheckPermission("user:password:reset")
  public ApiResponse<String> resetPassword(
      @PathVariable Integer userId, @Valid @RequestBody RequestResetPasswordDTO requestResetPasswordDTO) {
    return userService.resetPassword(userId, requestResetPasswordDTO);
  }

  @GetMapping("/{userId}/memberships")
  @SaCheckPermission("user:membership:list")
  public ApiResponse<List<ResponseMembershipDTO>> memberships(@PathVariable Integer userId) {
    return userService.getUserMemberships(userId);
  }
}
