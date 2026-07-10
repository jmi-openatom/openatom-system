package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

  /** 统计匿名申请中同一学号在同一招新批次内的有效申请数 */
  @Select("""
      SELECT COUNT(*) FROM membership_application
      WHERE campaign_id = #{campaignId}
        AND user_id IS NULL
        AND status != 'cancelled'
        AND JSON_UNQUOTE(JSON_EXTRACT(profile, '$.studentId')) = #{studentId}
      """)
  Long countActiveByCampaignAndStudentId(
      @Param("campaignId") Integer campaignId,
      @Param("studentId") String studentId);

  /** 将所有匿名申请中学号匹配的记录绑定到指定用户 */
  @Update("""
      UPDATE membership_application
      SET user_id = #{userId}
      WHERE user_id IS NULL
        AND JSON_UNQUOTE(JSON_EXTRACT(profile, '$.studentId')) = #{studentId}
      """)
  int bindAnonymousApplicationsByStudentId(
      @Param("userId") Integer userId,
      @Param("studentId") String studentId);

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
      String keyword,
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
        .and(
            keyword != null && !keyword.isBlank(),
            query -> {
              if (userIds != null && !userIds.isEmpty()) {
                query.in(MembershipApplication::getUserId, userIds).or();
              }
              query.like(MembershipApplication::getProfile, keyword.trim());
            })
        .orderByDesc(MembershipApplication::getId);
    return selectPage(page, wrapper);
  }
  /** 按条件查询所有申请（不分页，用于导出） */
  default List<MembershipApplication> selectAllByConditions(
      Integer campaignId, Integer clubId, String status, Integer departmentId, String keyword, List<Integer> userIds) {
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
        .and(
            keyword != null && !keyword.isBlank(),
            query -> {
              if (userIds != null && !userIds.isEmpty()) {
                query.in(MembershipApplication::getUserId, userIds).or();
              }
              query.like(MembershipApplication::getProfile, keyword.trim());
            })
        .orderByDesc(MembershipApplication::getId);
    return selectList(wrapper);
  }

  /** 启动时批量补绑：将所有未绑定申请按学号匹配到已有用户 */
  @Update("""
      UPDATE membership_application a
      INNER JOIN tb_user u
        ON JSON_UNQUOTE(JSON_EXTRACT(a.profile, '$.studentId')) = u.student_id
      SET a.user_id = u.id
      WHERE a.user_id IS NULL
        AND JSON_UNQUOTE(JSON_EXTRACT(a.profile, '$.studentId')) IS NOT NULL
        AND JSON_UNQUOTE(JSON_EXTRACT(a.profile, '$.studentId')) != ''
      """)
  int bindAllOrphanApplications();

  /** 清空 userId (用户被删除时) */
  default void nullifyUserId(Integer userId) {
    update(
        null,
        new LambdaUpdateWrapper<MembershipApplication>()
            .eq(MembershipApplication::getUserId, userId)
            .set(MembershipApplication::getUserId, null));
  }
}
