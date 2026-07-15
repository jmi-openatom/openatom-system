package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfile;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberProfileMapper extends BaseMapper<MemberProfile> {
  default MemberProfile selectByUserId(Integer userId) {
    return selectOne(
        new LambdaQueryWrapper<MemberProfile>()
            .eq(MemberProfile::getUserId, userId)
            .last("LIMIT 1"));
  }

  default MemberProfile selectBySlug(String slug) {
    return selectOne(
        new LambdaQueryWrapper<MemberProfile>()
            .eq(MemberProfile::getSlug, slug)
            .last("LIMIT 1"));
  }

  default Long countBySlug(String slug, Long excludeId) {
    LambdaQueryWrapper<MemberProfile> wrapper =
        new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getSlug, slug);
    if (excludeId != null) wrapper.ne(MemberProfile::getId, excludeId);
    return selectCount(wrapper);
  }

  default int updateWithVersion(MemberProfile profile, Integer expectedVersion) {
    return update(
        profile,
        new LambdaUpdateWrapper<MemberProfile>()
            .eq(MemberProfile::getId, profile.getId())
            .eq(MemberProfile::getVersion, expectedVersion));
  }

  default List<MemberProfile> selectByUserIds(List<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) return List.of();
    return selectList(
        new LambdaQueryWrapper<MemberProfile>().in(MemberProfile::getUserId, userIds));
  }

  default List<MemberProfile> searchPublished(String keyword) {
    LambdaQueryWrapper<MemberProfile> wrapper =
        new LambdaQueryWrapper<MemberProfile>()
            .eq(MemberProfile::getStatus, "published")
            .eq(MemberProfile::getVisibility, "members");
    if (keyword != null && !keyword.isBlank()) {
      String value = keyword.trim();
      wrapper.and(
          query ->
              query
                  .like(MemberProfile::getDisplayName, value)
                  .or()
                  .like(MemberProfile::getHeadline, value)
                  .or()
                  .like(MemberProfile::getSkills, value));
    }
    return selectList(wrapper);
  }
}
