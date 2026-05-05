package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreatePermissionDTO;
import edu.jmi.openatom.server.openatomsystem.mapper.PermissionMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import edu.jmi.openatom.server.openatomsystem.service.PermissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
  private final PermissionMapper permissionMapper;

  @Override
  public ApiResponse<List<Permission>> getPermissions() {
    return ApiResponse.success(permissionMapper.selectList(null));
  }

  @Override
  public ApiResponse<String> createPermission(RequestCreatePermissionDTO requestCreatePermissionDTO) {
    if (requestCreatePermissionDTO == null) {
      return ApiResponse.error("请求参数为空");
    }
    if (isBlank(requestCreatePermissionDTO.getName())
        || isBlank(requestCreatePermissionDTO.getCode())
        || isBlank(requestCreatePermissionDTO.getType())) {
      return ApiResponse.error(400, "权限名称、权限编码、权限类型不能为空");
    }
    if (permissionMapper.selectCount(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getCode, requestCreatePermissionDTO.getCode()))
        > 0) {
      return ApiResponse.error(400, "权限编码已存在");
    }

    Permission permission =
        Permission.builder()
            .name(requestCreatePermissionDTO.getName())
            .code(requestCreatePermissionDTO.getCode())
            .type(requestCreatePermissionDTO.getType())
            .path(requestCreatePermissionDTO.getPath())
            .method(requestCreatePermissionDTO.getMethod())
            .build();

    int row = permissionMapper.insert(permission);
    return row > 0 ? ApiResponse.success("权限创建成功") : ApiResponse.error("权限创建失败");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
