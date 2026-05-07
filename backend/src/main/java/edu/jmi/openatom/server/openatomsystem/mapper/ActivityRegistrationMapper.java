package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ActivityRegistration;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {

  /** 查用户的活动报名 */
  default ActivityRegistration selectOneByActivityAndUser(Integer activityId, Integer userId) {
    return selectOne(
        new LambdaQueryWrapper<ActivityRegistration>()
            .eq(ActivityRegistration::getActivityId, activityId)
            .eq(ActivityRegistration::getUserId, userId)
            .last("LIMIT 1"));
  }

  /** 按活动ID查报名（ordered by createdAt desc） */
  default List<ActivityRegistration> selectByActivityIdOrdered(Integer activityId) {
    return selectList(
        new LambdaQueryWrapper<ActivityRegistration>()
            .eq(ActivityRegistration::getActivityId, activityId)
            .orderByDesc(ActivityRegistration::getCreatedAt));
  }

  /** 按 userId 删除 */
  default int deleteByUserId(Integer userId) {
    return delete(
        new LambdaQueryWrapper<ActivityRegistration>()
            .eq(ActivityRegistration::getUserId, userId));
  }
}
