package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志数据访问层
 *
 * <p>提供对登录日志(LoginLog)的数据库操作, 包括查询所有登录日志并按登录时间降序排序等功能
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

  /** 查所有登录日志（ordered by loginAt desc） */
  default Page<LoginLog> selectPageByKeyword(Page<LoginLog> page, String keyword) {
    LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
    String trimmedKeyword = keyword == null ? null : keyword.trim();
    Integer keywordUserId = parseInteger(trimmedKeyword);
    wrapper.and(
        trimmedKeyword != null && !trimmedKeyword.isBlank(),
        query -> {
          query.like(LoginLog::getIp, trimmedKeyword).or().like(LoginLog::getUserAgent, trimmedKeyword);
          if (keywordUserId != null) {
            query.or().eq(LoginLog::getUserId, keywordUserId);
          }
        });
    wrapper.orderByDesc(LoginLog::getLoginAt).orderByDesc(LoginLog::getId);
    return selectPage(page, wrapper);
  }

  private Integer parseInteger(String value) {
    if (value == null || !value.matches("\\d+")) {
      return null;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
