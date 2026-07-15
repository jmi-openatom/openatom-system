package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileLike;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberProfileLikeMapper extends BaseMapper<MemberProfileLike> {
  default MemberProfileLike selectByTargetAndUser(Integer profileUserId, Integer userId) {
    return selectOne(
        new LambdaQueryWrapper<MemberProfileLike>()
            .eq(MemberProfileLike::getProfileUserId, profileUserId)
            .eq(MemberProfileLike::getUserId, userId)
            .last("LIMIT 1"));
  }

  default List<MemberProfileLike> selectByProfileUserIds(List<Integer> profileUserIds) {
    if (profileUserIds == null || profileUserIds.isEmpty()) return List.of();
    return selectList(
        new LambdaQueryWrapper<MemberProfileLike>()
            .in(MemberProfileLike::getProfileUserId, profileUserIds));
  }

  default Long countByProfileUserId(Integer profileUserId) {
    return selectCount(
        new LambdaQueryWrapper<MemberProfileLike>()
            .eq(MemberProfileLike::getProfileUserId, profileUserId));
  }
}
