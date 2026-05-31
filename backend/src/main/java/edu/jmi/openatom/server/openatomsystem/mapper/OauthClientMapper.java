package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OauthClientMapper extends BaseMapper<OauthClient> {
  default OauthClient selectByClientId(String clientId) {
    if (clientId == null || clientId.isBlank()) return null;
    return selectOne(new LambdaQueryWrapper<OauthClient>().eq(OauthClient::getClientId, clientId).last("LIMIT 1"));
  }
}
