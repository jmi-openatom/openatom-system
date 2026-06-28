package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAlumniGroup;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubAlumniGroupMapper;
import edu.jmi.openatom.server.openatomsystem.service.AlumniGroupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 往届管理人员分组服务实现
 */
@Service
@RequiredArgsConstructor
public class AlumniGroupServiceImpl implements AlumniGroupService {

  private final ClubAlumniGroupMapper alumniGroupMapper;

  @Override
  public Result<List<ClubAlumniGroup>> listByClubId(Integer clubId) {
    return Result.success(alumniGroupMapper.selectByClubIdOrdered(clubId));
  }

  @Override
  public Result<String> create(Integer clubId, RequestCreateAlumniGroupDTO request) {
    long exists = alumniGroupMapper.selectCount(
        new LambdaQueryWrapper<ClubAlumniGroup>()
            .eq(ClubAlumniGroup::getClubId, clubId)
            .eq(ClubAlumniGroup::getName, request.getName()));
    if (exists > 0) return Result.error(409, "分组名称已存在");
    alumniGroupMapper.insert(ClubAlumniGroup.builder()
        .clubId(clubId)
        .name(request.getName())
        .description(request.getDescription())
        .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
        .build());
    return Result.success("分组创建成功");
  }

  @Override
  public Result<String> update(Integer groupId, RequestUpdateAlumniGroupDTO request) {
    ClubAlumniGroup group = alumniGroupMapper.selectById(groupId);
    if (group == null) return Result.error(404, "分组不存在");
    if (request.getName() != null) {
      if (request.getName().isBlank()) return Result.error(400, "分组名称不能为空");
      long dup = alumniGroupMapper.selectCount(
          new LambdaQueryWrapper<ClubAlumniGroup>()
              .eq(ClubAlumniGroup::getClubId, group.getClubId())
              .eq(ClubAlumniGroup::getName, request.getName())
              .ne(ClubAlumniGroup::getId, groupId));
      if (dup > 0) return Result.error(409, "分组名称已存在");
      group.setName(request.getName());
    }
    if (request.getDescription() != null) group.setDescription(request.getDescription());
    if (request.getSortOrder() != null) group.setSortOrder(request.getSortOrder());
    alumniGroupMapper.updateById(group);
    return Result.success("分组更新成功");
  }

  @Override
  public Result<String> delete(Integer groupId) {
    ClubAlumniGroup group = alumniGroupMapper.selectById(groupId);
    if (group == null) return Result.error(404, "分组不存在");
    alumniGroupMapper.deleteById(groupId);
    return Result.success("分组删除成功");
  }
}
