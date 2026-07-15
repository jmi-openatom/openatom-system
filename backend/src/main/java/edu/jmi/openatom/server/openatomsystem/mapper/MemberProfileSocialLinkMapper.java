package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileSocialLink;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberProfileSocialLinkMapper extends BaseMapper<MemberProfileSocialLink> {
  default List<MemberProfileSocialLink> selectByProfileId(Long profileId) {
    return selectList(
        new LambdaQueryWrapper<MemberProfileSocialLink>()
            .eq(MemberProfileSocialLink::getProfileId, profileId)
            .orderByAsc(MemberProfileSocialLink::getSortOrder)
            .orderByAsc(MemberProfileSocialLink::getId));
  }

  default int deleteByProfileId(Long profileId) {
    return delete(
        new LambdaQueryWrapper<MemberProfileSocialLink>()
            .eq(MemberProfileSocialLink::getProfileId, profileId));
  }
}
