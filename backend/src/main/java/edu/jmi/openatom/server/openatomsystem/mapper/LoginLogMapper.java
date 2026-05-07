package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

  /** 查所有登录日志（ordered by loginAt desc） */
  default List<LoginLog> selectAllOrdered() {
    return selectList(
        new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getLoginAt));
  }
}
