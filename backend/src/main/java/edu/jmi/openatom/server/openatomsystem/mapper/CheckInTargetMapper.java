package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInTarget;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInTargetMapper extends BaseMapper<CheckInTarget> {
  default List<CheckInTarget> selectBySessionId(Integer sessionId) {
    return selectList(new LambdaQueryWrapper<CheckInTarget>().eq(CheckInTarget::getSessionId, sessionId));
  }

  default CheckInTarget selectOneBySessionAndUser(Integer sessionId, Integer userId) {
    return selectOne(
        new LambdaQueryWrapper<CheckInTarget>()
            .eq(CheckInTarget::getSessionId, sessionId)
            .eq(CheckInTarget::getUserId, userId)
            .last("LIMIT 1"));
  }

  default int deleteBySessionId(Integer sessionId) {
    return delete(new LambdaQueryWrapper<CheckInTarget>().eq(CheckInTarget::getSessionId, sessionId));
  }
}
