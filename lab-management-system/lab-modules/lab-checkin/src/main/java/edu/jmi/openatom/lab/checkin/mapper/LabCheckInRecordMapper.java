package edu.jmi.openatom.lab.checkin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabCheckInRecordMapper extends BaseMapper<LabCheckInRecord> {
  default LabCheckInRecord selectOneBySessionAndUser(Long sessionId, Long userId) {
    return selectOne(
        new LambdaQueryWrapper<LabCheckInRecord>()
            .eq(LabCheckInRecord::getSessionId, sessionId)
            .eq(LabCheckInRecord::getUserId, userId)
            .last("LIMIT 1"));
  }

  default List<LabCheckInRecord> selectBySessionId(Long sessionId) {
    return selectList(
        new LambdaQueryWrapper<LabCheckInRecord>()
            .eq(LabCheckInRecord::getSessionId, sessionId)
            .orderByDesc(LabCheckInRecord::getCheckinAt));
  }

  default int deleteBySessionId(Long sessionId) {
    return delete(new LambdaQueryWrapper<LabCheckInRecord>().eq(LabCheckInRecord::getSessionId, sessionId));
  }
}
