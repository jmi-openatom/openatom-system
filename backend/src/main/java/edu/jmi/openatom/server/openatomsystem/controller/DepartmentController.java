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

/**
 * 部门控制器
 *
 * <p>提供社团部门的增删改查操作
 */
@RestController
@RequiredArgsConstructor
public class DepartmentController {
  private final DepartmentService departmentService;

  /**
   * 根据社团ID获取部门列表
   *
   * @param clubId 社团ID
   * @return 部门列表
   */
  @GetMapping("/clubs/{clubId}/departments")
  public ApiResponse<List<ClubDepartment>> getDepartmentsByClubId(@PathVariable Integer clubId) {
    return departmentService.getDepartmentsByClubId(clubId);
  }

  /**
   * 创建部门
   *
   * @param clubId 社团ID
   * @param requestCreateDepartmentDTO 创建部门请求
   * @return 操作结果
   */
  @PostMapping("/clubs/{clubId}/departments")
  @SaCheckPermission("department:create")
  public ApiResponse<String> createDepartment(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestCreateDepartmentDTO requestCreateDepartmentDTO) {
    return departmentService.createDepartment(clubId, requestCreateDepartmentDTO);
  }

  /**
   * 根据ID获取部门详情
   *
   * @param departmentId 部门ID
   * @return 部门详情
   */
  @GetMapping("/departments/{departmentId}")
  @SaCheckPermission("department:detail")
  public ApiResponse<ClubDepartment> getDepartmentById(@PathVariable Integer departmentId) {
    return departmentService.getDepartmentById(departmentId);
  }

  /**
   * 更新部门信息
   *
   * @param departmentId 部门ID
   * @param requestUpdateDepartmentDTO 更新部门请求
   * @return 操作结果
   */
  @PatchMapping("/departments/{departmentId}")
  @SaCheckPermission("department:update")
  public ApiResponse<String> updateDepartment(
      @PathVariable Integer departmentId,
      @Valid @RequestBody RequestUpdateDepartmentDTO requestUpdateDepartmentDTO) {
    return departmentService.updateDepartment(departmentId, requestUpdateDepartmentDTO);
  }

  /**
   * 删除部门
   *
   * @param departmentId 部门ID
   * @return 操作结果
   */
  @DeleteMapping("/departments/{departmentId}")
  @SaCheckPermission("department:delete")
  public ApiResponse<String> deleteDepartment(@PathVariable Integer departmentId) {
    return departmentService.deleteDepartment(departmentId);
  }
}
