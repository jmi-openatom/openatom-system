package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInExclusion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInExclusionMapper extends BaseMapper<CheckInExclusion> {
  default List<CheckInExclusion> selectBySessionId(Integer sessionId) {
    return selectList(new LambdaQueryWrapper<CheckInExclusion>().eq(CheckInExclusion::getSessionId, sessionId));
  }

  default CheckInExclusion selectOneBySessionAndUser(Integer sessionId, Integer userId) {
    return selectOne(
        new LambdaQueryWrapper<CheckInExclusion>()
            .eq(CheckInExclusion::getSessionId, sessionId)
            .eq(CheckInExclusion::getUserId, userId)
            .last("LIMIT 1"));
  }

  default int deleteBySessionId(Integer sessionId) {
    return delete(new LambdaQueryWrapper<CheckInExclusion>().eq(CheckInExclusion::getSessionId, sessionId));
  }

  default int deleteBySessionIdAndUserId(Integer sessionId, Integer userId) {
    return delete(
        new LambdaQueryWrapper<CheckInExclusion>()
            .eq(CheckInExclusion::getSessionId, sessionId)
            .eq(CheckInExclusion::getUserId, userId));
  }
}
