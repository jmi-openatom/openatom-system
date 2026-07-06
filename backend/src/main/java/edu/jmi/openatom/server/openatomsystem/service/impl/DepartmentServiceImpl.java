package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCacheEvict;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCached;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.service.DepartmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 部门管理实现类
 *
 * <p>负责社团部门的创建, 更新, 删除和查询等业务逻辑, 包含部门名称排重校验
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;
  private final UserMapper userMapper;

  @Override
  @RedisCached(cacheName = "lookup:department", key = "'club:' + #p0", ttlSeconds = 1800)
  public Result<List<ClubDepartment>> getDepartmentsByClubId(Integer clubId) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (!clubExists(clubId)) return Result.error(404, "社团不存在");
    return Result.success(clubDepartmentMapper.selectByClubIdOrdered(clubId));
  }

  @Override
  @RedisCacheEvict(cacheNames = {"lookup:department", "site"})
  public Result<String> createDepartment(Integer clubId, RequestCreateDepartmentDTO dto) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (dto == null) return Result.error("请求参数为空");
    if (isBlank(dto.getName())) return Result.error(400, "部门名称不能为空");
    if (!clubExists(clubId)) return Result.error(404, "社团不存在");
    if (!userExists(dto.getManagerUserId())) return Result.error(400, "负责人用户不存在");
    if (dto.getViceManagerUserId() != null && dto.getViceManagerUserId() != 0
        && !userExists(dto.getViceManagerUserId())) return Result.error(400, "副部长用户不存在");
    if (clubDepartmentMapper.countByClubIdAndName(clubId, dto.getName(), null) > 0)
      return Result.error(400, "部门名称已存在");
    ClubDepartment department = ClubDepartment.builder().clubId(clubId).name(dto.getName())
        .description(dto.getDescription()).managerUserId(dto.getManagerUserId())
        .viceManagerUserId(dto.getViceManagerUserId()).build();
    int row = clubDepartmentMapper.insert(department);
    return row > 0 ? Result.success("部门创建成功") : Result.error("部门创建失败");
  }

  @Override
  @RedisCached(cacheName = "lookup:department", key = "'detail:' + #p0", ttlSeconds = 1800)
  public Result<ClubDepartment> getDepartmentById(Integer departmentId) {
    if (departmentId == null) return Result.error(400, "departmentId不能为空");
    ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
    if (department == null) return Result.error(404, "部门不存在");
    return Result.success(department);
  }

  @Override
  @RedisCacheEvict(cacheNames = {"lookup:department", "site"})
  public Result<String> updateDepartment(Integer departmentId, RequestUpdateDepartmentDTO dto) {
    if (departmentId == null) return Result.error(400, "departmentId不能为空");
    if (dto == null) return Result.error("请求参数为空");
    ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
    if (department == null) return Result.error(404, "部门不存在");
    if (dto.getName() != null) {
      if (isBlank(dto.getName())) return Result.error(400, "部门名称不能为空");
      if (clubDepartmentMapper.countByClubIdAndName(department.getClubId(), dto.getName(), departmentId) > 0)
        return Result.error(400, "部门名称已存在");
      department.setName(dto.getName());
    }
    if (dto.getDescription() != null) department.setDescription(dto.getDescription());
    if (dto.getManagerUserId() != null) {
      if (dto.getManagerUserId() != 0 && !userExists(dto.getManagerUserId())) return Result.error(400, "负责人用户不存在");
      department.setManagerUserId(dto.getManagerUserId() == 0 ? null : dto.getManagerUserId());
    }
    if (dto.getViceManagerUserId() != null) {
      if (dto.getViceManagerUserId() != 0 && !userExists(dto.getViceManagerUserId())) return Result.error(400, "副部长用户不存在");
      department.setViceManagerUserId(dto.getViceManagerUserId() == 0 ? null : dto.getViceManagerUserId());
    }
    if (dto.getWechatGroupQrcode() != null) {
      department.setWechatGroupQrcode(dto.getWechatGroupQrcode());
    }
    int row = clubDepartmentMapper.updateById(department);
    return row > 0 ? Result.success("部门更新成功") : Result.error("部门更新失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @RedisCacheEvict(cacheNames = {"lookup:department", "site"})
  public Result<String> deleteDepartment(Integer departmentId) {
    if (departmentId == null) return Result.error(400, "departmentId不能为空");
    ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
    if (department == null) return Result.error(404, "部门不存在");
    int row = clubDepartmentMapper.deleteById(departmentId);
    return row > 0 ? Result.success("部门删除成功") : Result.error("部门删除失败");
  }

  private boolean clubExists(Integer clubId) { return clubMapper.selectById(clubId) != null; }
  private boolean userExists(Integer userId) { return userId == null || userMapper.selectById(userId) != null; }
  private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
