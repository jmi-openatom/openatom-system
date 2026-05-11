package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入社申请数据访问层
 *
 * <p>提供对入社申请(MembershipApplication)的数据库操作, 包括按用户ID查询申请, 统计同批次有效申请数, 按招新计划查询申请, 条件分页查询以及清空申请用户ID等功能
 */
@Mapper
public interface MembershipApplicationMapper extends BaseMapper<MembershipApplication> {

  /** 按 userId 查申请（orderByDesc id） */
  default List<MembershipApplication> selectByUserIdOrdered(Integer userId) {
    return selectList(
        new LambdaQueryWrapper<MembershipApplication>()
            .eq(MembershipApplication::getUserId, userId)
            .orderByDesc(MembershipApplication::getId));
  }

  /** 统计同批次有效申请数（排除已取消的） */
  default Long countActiveByCampaignAndUser(Integer campaignId, Integer userId) {
    return selectCount(
        new LambdaQueryWrapper<MembershipApplication>()
            .eq(MembershipApplication::getCampaignId, campaignId)
            .eq(MembershipApplication::getUserId, userId)
            .ne(MembershipApplication::getStatus, "cancelled"));
  }

  /** 按招新计划查申请 */
  default List<MembershipApplication> selectByCampaignId(Integer campaignId) {
    return selectList(
        new LambdaQueryWrapper<MembershipApplication>()
            .eq(MembershipApplication::getCampaignId, campaignId));
  }

  /** 条件查询申请分页 */
  default Page<MembershipApplication> selectPageByConditions(
      Page<MembershipApplication> page,
      Integer campaignId,
      Integer clubId,
      String status,
      Integer departmentId,
      List<Integer> userIds) {
    LambdaQueryWrapper<MembershipApplication> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(campaignId != null, MembershipApplication::getCampaignId, campaignId)
        .eq(clubId != null, MembershipApplication::getClubId, clubId)
        .eq(status != null && !status.isBlank(), MembershipApplication::getStatus, status)
        .and(
            departmentId != null,
            query ->
                query
                    .eq(MembershipApplication::getFirstChoiceDepartmentId, departmentId)
                    .or()
                    .eq(MembershipApplication::getSecondChoiceDepartmentId, departmentId))
        .in(userIds != null && !userIds.isEmpty(), MembershipApplication::getUserId, userIds)
        .orderByDesc(MembershipApplication::getId);
    return selectPage(page, wrapper);
  }
  /** 清空 userId (用户被删除时) */
  default void nullifyUserId(Integer userId) {
    update(
        null,
        new LambdaUpdateWrapper<MembershipApplication>()
            .eq(MembershipApplication::getUserId, userId)
            .set(MembershipApplication::getUserId, null));
  }
}
