package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import java.util.List;

/**
 * 部门管理服务接口
 *
 * <p>定义社团部门的按社团查询列表, 创建, 查看详情, 更新和删除等业务操作
 */
public interface DepartmentService {
  Result<List<ClubDepartment>> getDepartmentsByClubId(Integer clubId);

  Result<String> createDepartment(
      Integer clubId, RequestCreateDepartmentDTO requestCreateDepartmentDTO);

  Result<ClubDepartment> getDepartmentById(Integer departmentId);

  Result<String> updateDepartment(
      Integer departmentId, RequestUpdateDepartmentDTO requestUpdateDepartmentDTO);

  Result<String> deleteDepartment(Integer departmentId);
}
