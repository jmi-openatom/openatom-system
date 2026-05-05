package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import java.util.List;

public interface DepartmentService {
  ApiResponse<List<ClubDepartment>> getDepartmentsByClubId(Integer clubId);

  ApiResponse<String> createDepartment(
      Integer clubId, RequestCreateDepartmentDTO requestCreateDepartmentDTO);

  ApiResponse<ClubDepartment> getDepartmentById(Integer departmentId);

  ApiResponse<String> updateDepartment(
      Integer departmentId, RequestUpdateDepartmentDTO requestUpdateDepartmentDTO);

  ApiResponse<String> deleteDepartment(Integer departmentId);
}
