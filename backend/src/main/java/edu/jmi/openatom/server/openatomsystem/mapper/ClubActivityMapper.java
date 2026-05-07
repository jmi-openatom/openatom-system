package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClubActivityMapper extends BaseMapper<ClubActivity> {

  /** 最新已发布活动（限数量） */
  default List<ClubActivity> selectPublishedByClubId(Integer clubId, int limit) {
    return selectList(
        new LambdaQueryWrapper<ClubActivity>()
            .eq(ClubActivity::getClubId, clubId)
            .eq(ClubActivity::getStatus, "published")
            .orderByDesc(ClubActivity::getActivityAt)
            .last("LIMIT " + limit));
  }

  /** 所有已发布活动 */
  default List<ClubActivity> selectPublishedByClubIdAll(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubActivity>()
            .eq(ClubActivity::getClubId, clubId)
            .eq(ClubActivity::getStatus, "published")
            .orderByDesc(ClubActivity::getActivityAt)
            .orderByDesc(ClubActivity::getId));
  }

  /** 按社团和状态查活动 */
  default List<ClubActivity> selectByClubIdAndStatus(Integer clubId, String status) {
    LambdaQueryWrapper<ClubActivity> wrapper =
        new LambdaQueryWrapper<ClubActivity>()
            .eq(ClubActivity::getClubId, clubId)
            .orderByDesc(ClubActivity::getActivityAt)
            .orderByDesc(ClubActivity::getId);
    if (status != null && !status.isBlank()) {
      wrapper.eq(ClubActivity::getStatus, status);
    }
    return selectList(wrapper);
  }

  /** 按活动ID和社团ID查单条 */
  default ClubActivity selectOneByIdAndClubId(Integer activityId, Integer clubId) {
    return selectOne(
        new LambdaQueryWrapper<ClubActivity>()
            .eq(ClubActivity::getId, activityId)
            .eq(ClubActivity::getClubId, clubId)
            .last("LIMIT 1"));
  }

  /** 按活动ID和社团ID查已发布的单条 */
  default ClubActivity selectPublishedByIdAndClubId(Integer activityId, Integer clubId) {
    return selectOne(
        new LambdaQueryWrapper<ClubActivity>()
            .eq(ClubActivity::getId, activityId)
            .eq(ClubActivity::getClubId, clubId)
            .eq(ClubActivity::getStatus, "published")
            .last("LIMIT 1"));
  }
}
