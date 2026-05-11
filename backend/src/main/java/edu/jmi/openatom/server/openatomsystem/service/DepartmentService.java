package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import java.util.List;

/**
 * 部门管理服务接口
 *
 * <p>定义社团部门的按社团查询列表, 创建, 查看详情, 更新和删除等业务操作
 */
public interface DepartmentService {
  ApiResponse<List<ClubDepartment>> getDepartmentsByClubId(Integer clubId);

  ApiResponse<String> createDepartment(
      Integer clubId, RequestCreateDepartmentDTO requestCreateDepartmentDTO);

  ApiResponse<ClubDepartment> getDepartmentById(Integer departmentId);

  ApiResponse<String> updateDepartment(
      Integer departmentId, RequestUpdateDepartmentDTO requestUpdateDepartmentDTO);

  ApiResponse<String> deleteDepartment(Integer departmentId);
}
