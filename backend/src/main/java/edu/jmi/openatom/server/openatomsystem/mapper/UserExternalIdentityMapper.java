package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.UserExternalIdentity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserExternalIdentityMapper extends BaseMapper<UserExternalIdentity> {
  default UserExternalIdentity selectByProviderSubject(String provider, String subject) {
    return selectOne(new LambdaQueryWrapper<UserExternalIdentity>()
        .eq(UserExternalIdentity::getProvider, provider)
        .eq(UserExternalIdentity::getSubject, subject).last("LIMIT 1"));
  }

  default UserExternalIdentity selectByUserProvider(Integer userId, String provider) {
    return selectOne(new LambdaQueryWrapper<UserExternalIdentity>()
        .eq(UserExternalIdentity::getUserId, userId)
        .eq(UserExternalIdentity::getProvider, provider).last("LIMIT 1"));
  }
}
