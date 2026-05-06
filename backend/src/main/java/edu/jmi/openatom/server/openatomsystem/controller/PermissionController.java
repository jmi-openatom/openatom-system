package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreatePermissionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import edu.jmi.openatom.server.openatomsystem.service.PermissionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/permissions")
public class PermissionController {
  private final PermissionService permissionService;

  @GetMapping
  @SaCheckPermission("permission:list")
  public ApiResponse<List<Permission>> getPermissions() {
    return permissionService.getPermissions();
  }

  @PostMapping
  @SaCheckPermission("permission:create")
  public ApiResponse<String> createPermission(
      @Valid @RequestBody RequestCreatePermissionDTO requestCreatePermissionDTO) {
    return permissionService.createPermission(requestCreatePermissionDTO);
  }
}
