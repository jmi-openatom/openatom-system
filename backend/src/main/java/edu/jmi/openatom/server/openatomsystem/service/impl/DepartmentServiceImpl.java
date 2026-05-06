package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.service.DepartmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;
  private final UserMapper userMapper;

  @Override
  public ApiResponse<List<ClubDepartment>> getDepartmentsByClubId(Integer clubId) {
    if (clubId == null) {
      return ApiResponse.error(400, "clubId不能为空");
    }
    if (!clubExists(clubId)) {
      return ApiResponse.error(404, "社团不存在");
    }

    List<ClubDepartment> departments =
        clubDepartmentMapper.selectList(
            new LambdaQueryWrapper<ClubDepartment>()
                .eq(ClubDepartment::getClubId, clubId)
                .orderByAsc(ClubDepartment::getId));
    return ApiResponse.success(departments);
  }

  @Override
  public ApiResponse<String> createDepartment(
      Integer clubId, RequestCreateDepartmentDTO requestCreateDepartmentDTO) {
    if (clubId == null) {
      return ApiResponse.error(400, "clubId不能为空");
    }
    if (requestCreateDepartmentDTO == null) {
      return ApiResponse.error("请求参数为空");
    }
    if (isBlank(requestCreateDepartmentDTO.getName())) {
      return ApiResponse.error(400, "部门名称不能为空");
    }
    if (!clubExists(clubId)) {
      return ApiResponse.error(404, "社团不存在");
    }
    if (!userExists(requestCreateDepartmentDTO.getManagerUserId())) {
      return ApiResponse.error(400, "负责人用户不存在");
    }
    if (departmentNameExists(clubId, requestCreateDepartmentDTO.getName(), null)) {
      return ApiResponse.error(400, "部门名称已存在");
    }

    ClubDepartment department =
        ClubDepartment.builder()
            .clubId(clubId)
            .name(requestCreateDepartmentDTO.getName())
            .description(requestCreateDepartmentDTO.getDescription())
            .managerUserId(requestCreateDepartmentDTO.getManagerUserId())
            .build();

    int row = clubDepartmentMapper.insert(department);
    return row > 0 ? ApiResponse.success("部门创建成功") : ApiResponse.error("部门创建失败");
  }

  @Override
  public ApiResponse<ClubDepartment> getDepartmentById(Integer departmentId) {
    if (departmentId == null) {
      return ApiResponse.error(400, "departmentId不能为空");
    }

    ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
    if (department == null) {
      return ApiResponse.error(404, "部门不存在");
    }
    return ApiResponse.success(department);
  }

  @Override
  public ApiResponse<String> updateDepartment(
      Integer departmentId, RequestUpdateDepartmentDTO requestUpdateDepartmentDTO) {
    if (departmentId == null) {
      return ApiResponse.error(400, "departmentId不能为空");
    }
    if (requestUpdateDepartmentDTO == null) {
      return ApiResponse.error("请求参数为空");
    }

    ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
    if (department == null) {
      return ApiResponse.error(404, "部门不存在");
    }
    if (requestUpdateDepartmentDTO.getName() != null) {
      if (isBlank(requestUpdateDepartmentDTO.getName())) {
        return ApiResponse.error(400, "部门名称不能为空");
      }
      if (departmentNameExists(
          department.getClubId(), requestUpdateDepartmentDTO.getName(), departmentId)) {
        return ApiResponse.error(400, "部门名称已存在");
      }
      department.setName(requestUpdateDepartmentDTO.getName());
    }
    if (requestUpdateDepartmentDTO.getDescription() != null) {
      department.setDescription(requestUpdateDepartmentDTO.getDescription());
    }
    if (requestUpdateDepartmentDTO.getManagerUserId() != null) {
      if (!userExists(requestUpdateDepartmentDTO.getManagerUserId())) {
        return ApiResponse.error(400, "负责人用户不存在");
      }
      department.setManagerUserId(requestUpdateDepartmentDTO.getManagerUserId());
    }

    int row = clubDepartmentMapper.updateById(department);
    return row > 0 ? ApiResponse.success("部门更新成功") : ApiResponse.error("部门更新失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> deleteDepartment(Integer departmentId) {
    if (departmentId == null) {
      return ApiResponse.error(400, "departmentId不能为空");
    }

    ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
    if (department == null) {
      return ApiResponse.error(404, "部门不存在");
    }

    int row = clubDepartmentMapper.deleteById(departmentId);
    return row > 0 ? ApiResponse.success("部门删除成功") : ApiResponse.error("部门删除失败");
  }

  private boolean clubExists(Integer clubId) {
    Club club = clubMapper.selectById(clubId);
    return club != null;
  }

  private boolean userExists(Integer userId) {
    return userId == null || userMapper.selectById(userId) != null;
  }

  private boolean departmentNameExists(Integer clubId, String name, Integer excludeDepartmentId) {
    LambdaQueryWrapper<ClubDepartment> wrapper =
        new LambdaQueryWrapper<ClubDepartment>()
            .eq(ClubDepartment::getClubId, clubId)
            .eq(ClubDepartment::getName, name);
    if (excludeDepartmentId != null) {
      wrapper.ne(ClubDepartment::getId, excludeDepartmentId);
    }
    return clubDepartmentMapper.selectCount(wrapper) > 0;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
