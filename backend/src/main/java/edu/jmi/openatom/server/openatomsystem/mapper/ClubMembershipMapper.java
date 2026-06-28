package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社团成员关系数据访问层
 *
 * <p>提供对社团成员关系(ClubMembership)的数据库操作, 包括按社团ID或用户ID查询成员列表, 统计活跃成员关系以及条件查询成员等功能
 */
@Mapper
public interface ClubMembershipMapper extends BaseMapper<ClubMembership> {

  /** 按 clubId 查成员列表（含排序：featured desc, sortOrder asc, joinedAt desc） */
  default List<ClubMembership> selectByClubIdOrdered(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubMembership>()
            .eq(ClubMembership::getClubId, clubId)
            .isNull(ClubMembership::getLeftAt)
            .orderByDesc(ClubMembership::getFeatured)
            .orderByAsc(ClubMembership::getSortOrder)
            .orderByDesc(ClubMembership::getJoinedAt));
  }

  /** 按 clubId 查成员列表（无排序） */
  default List<ClubMembership> selectByClubId(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubMembership>().eq(ClubMembership::getClubId, clubId));
  }

  /** 按 userId 查成员列表 */
  default List<ClubMembership> selectByUserId(Integer userId) {
    return selectList(
        new LambdaQueryWrapper<ClubMembership>().eq(ClubMembership::getUserId, userId));
  }

  /** 统计用户的活跃成员关系数（未退社） */
  default Long countActiveByUserId(Integer userId) {
    return selectCount(
        new LambdaQueryWrapper<ClubMembership>()
            .eq(ClubMembership::getUserId, userId)
            .isNull(ClubMembership::getLeftAt));
  }

  /** 统计 userId+clubId 的活跃成员关系（特定状态） */
  default Long countActiveMembership(Integer userId, Integer clubId, String status) {
    return selectCount(
        new LambdaQueryWrapper<ClubMembership>()
            .eq(ClubMembership::getUserId, userId)
            .eq(ClubMembership::getClubId, clubId)
            .eq(ClubMembership::getStatus, status)
            .isNull(ClubMembership::getLeftAt));
  }

  /** 查 userId+clubId 的活跃成员关系（未退社） */
  default ClubMembership selectActiveMembership(Integer userId, Integer clubId) {
    List<ClubMembership> list = selectList(
        new LambdaQueryWrapper<ClubMembership>()
            .eq(ClubMembership::getUserId, userId)
            .eq(ClubMembership::getClubId, clubId)
            .isNull(ClubMembership::getLeftAt)
            .orderByDesc(ClubMembership::getId)
            .last("LIMIT 1"));
    return list.isEmpty() ? null : list.get(0);
  }

  /** 查不是 left 状态的成员关系 */
  default ClubMembership selectActiveNotLeft(Integer userId, Integer clubId) {
    List<ClubMembership> list = selectList(
        new LambdaQueryWrapper<ClubMembership>()
            .eq(ClubMembership::getUserId, userId)
            .eq(ClubMembership::getClubId, clubId)
            .ne(ClubMembership::getStatus, "left")
            .orderByDesc(ClubMembership::getId)
            .last("LIMIT 1"));
    return list.isEmpty() ? null : list.get(0);
  }

  /** 统计不是 left 状态的成员关系数 */
  default Long countActiveNotLeft(Integer userId, Integer clubId) {
    return selectCount(
        new LambdaQueryWrapper<ClubMembership>()
            .eq(ClubMembership::getUserId, userId)
            .eq(ClubMembership::getClubId, clubId)
            .ne(ClubMembership::getStatus, "left"));
  }

  /** 按 userId 删除 */
  default int deleteByUserId(Integer userId) {
    return delete(
        new LambdaQueryWrapper<ClubMembership>().eq(ClubMembership::getUserId, userId));
  }

  /** 按 clubId 查询往届管理人员（已离社或已毕业且曾担任职务） */
  default List<ClubMembership> selectFormerManagersByClubId(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubMembership>()
            .eq(ClubMembership::getClubId, clubId)
            .in(ClubMembership::getStatus, "left", "graduated")
            .isNotNull(ClubMembership::getPositionId)
            .isNotNull(ClubMembership::getLeftAt)
            .orderByDesc(ClubMembership::getLeftAt));
  }

  /** 按条件查询成员列表 */
  default List<ClubMembership> selectByConditions(
      Integer clubId, Integer departmentId, Integer positionId, String status,
      List<Integer> userIds) {
    LambdaQueryWrapper<ClubMembership> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(clubId != null, ClubMembership::getClubId, clubId)
        .eq(departmentId != null, ClubMembership::getDepartmentId, departmentId)
        .eq(positionId != null, ClubMembership::getPositionId, positionId)
        .eq(status != null && !status.isBlank(), ClubMembership::getStatus, status)
        .orderByDesc(ClubMembership::getId);
    if (userIds != null) {
      wrapper.in(ClubMembership::getUserId, userIds);
    }
    return selectList(wrapper);
  }
}
