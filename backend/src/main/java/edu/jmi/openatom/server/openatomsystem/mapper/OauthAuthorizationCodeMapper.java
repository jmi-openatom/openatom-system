package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.OauthAuthorizationCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OauthAuthorizationCodeMapper extends BaseMapper<OauthAuthorizationCode> {
  default OauthAuthorizationCode selectByCode(String code) {
    if (code == null || code.isBlank()) return null;
    return selectOne(
        new LambdaQueryWrapper<OauthAuthorizationCode>()
            .eq(OauthAuthorizationCode::getCode, code)
            .last("LIMIT 1"));
  }
}
