package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreatePermissionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import edu.jmi.openatom.server.openatomsystem.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 *
 * <p>提供系统权限的查询与创建功能
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/permissions")
public class PermissionController {
	private final PermissionService permissionService;

	/**
	 * 获取所有权限
	 *
	 * @return 权限列表
	 */
	@GetMapping
	@SaCheckPermission("permission:list")
	public Result<List<Permission>> getPermissions() {
		return permissionService.getPermissions();
	}

	/**
	 * 创建新权限
	 *
	 * @param requestCreatePermissionDTO 创建权限请求参数
	 * @return 创建结果
	 */
	@PostMapping
	@SaCheckPermission("permission:create")
	public Result<String> createPermission(
			@Valid @RequestBody RequestCreatePermissionDTO requestCreatePermissionDTO) {
		return permissionService.createPermission(requestCreatePermissionDTO);
	}
}
