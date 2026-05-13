package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInRecordMapper extends BaseMapper<CheckInRecord> {
  default CheckInRecord selectOneBySessionAndUser(Integer sessionId, Integer userId) {
    return selectOne(
        new LambdaQueryWrapper<CheckInRecord>()
            .eq(CheckInRecord::getSessionId, sessionId)
            .eq(CheckInRecord::getUserId, userId)
            .last("LIMIT 1"));
  }

  default List<CheckInRecord> selectBySessionId(Integer sessionId) {
    return selectList(
        new LambdaQueryWrapper<CheckInRecord>()
            .eq(CheckInRecord::getSessionId, sessionId)
            .orderByDesc(CheckInRecord::getCheckinAt));
  }

  default int deleteBySessionId(Integer sessionId) {
    return delete(new LambdaQueryWrapper<CheckInRecord>().eq(CheckInRecord::getSessionId, sessionId));
  }
}
