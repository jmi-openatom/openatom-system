package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DepartmentController {
  private final DepartmentService departmentService;

  @GetMapping("/clubs/{clubId}/departments")
  @SaCheckPermission("department:list")
  public ApiResponse<List<ClubDepartment>> getDepartmentsByClubId(@PathVariable Integer clubId) {
    return departmentService.getDepartmentsByClubId(clubId);
  }

  @PostMapping("/clubs/{clubId}/departments")
  @SaCheckPermission("department:create")
  public ApiResponse<String> createDepartment(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestCreateDepartmentDTO requestCreateDepartmentDTO) {
    return departmentService.createDepartment(clubId, requestCreateDepartmentDTO);
  }

  @GetMapping("/departments/{departmentId}")
  @SaCheckPermission("department:detail")
  public ApiResponse<ClubDepartment> getDepartmentById(@PathVariable Integer departmentId) {
    return departmentService.getDepartmentById(departmentId);
  }

  @PatchMapping("/departments/{departmentId}")
  @SaCheckPermission("department:update")
  public ApiResponse<String> updateDepartment(
      @PathVariable Integer departmentId,
      @Valid @RequestBody RequestUpdateDepartmentDTO requestUpdateDepartmentDTO) {
    return departmentService.updateDepartment(departmentId, requestUpdateDepartmentDTO);
  }

  @DeleteMapping("/departments/{departmentId}")
  @SaCheckPermission("department:delete")
  public ApiResponse<String> deleteDepartment(@PathVariable Integer departmentId) {
    return departmentService.deleteDepartment(departmentId);
  }
}
