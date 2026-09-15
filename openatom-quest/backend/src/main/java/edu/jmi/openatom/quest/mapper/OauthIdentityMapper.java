package edu.jmi.openatom.quest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.quest.entity.OauthIdentity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OauthIdentityMapper extends BaseMapper<OauthIdentity> {

    @Select("SELECT * FROM quest_oauth_identity WHERE provider = #{provider} AND subject = #{subject} LIMIT 1")
    OauthIdentity findByProviderAndSubject(@Param("provider") String provider, @Param("subject") String subject);
}
