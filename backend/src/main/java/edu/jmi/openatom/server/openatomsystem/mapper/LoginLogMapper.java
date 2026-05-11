package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志数据访问层
 *
 * <p>提供对登录日志(LoginLog)的数据库操作, 包括查询所有登录日志并按登录时间降序排序等功能
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

  /** 查所有登录日志（ordered by loginAt desc） */
  default List<LoginLog> selectAllOrdered() {
    return selectList(
        new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getLoginAt));
  }
}
