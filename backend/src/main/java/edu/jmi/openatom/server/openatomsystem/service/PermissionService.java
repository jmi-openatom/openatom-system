package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreatePermissionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import java.util.List;

public interface PermissionService {
  ApiResponse<List<Permission>> getPermissions();

  ApiResponse<String> createPermission(RequestCreatePermissionDTO requestCreatePermissionDTO);
}
