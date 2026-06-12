package edu.jmi.openatom.lab.checkin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInTarget;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabCheckInTargetMapper extends BaseMapper<LabCheckInTarget> {
  default List<LabCheckInTarget> selectBySessionId(Long sessionId) {
    return selectList(new LambdaQueryWrapper<LabCheckInTarget>().eq(LabCheckInTarget::getSessionId, sessionId));
  }

  default LabCheckInTarget selectOneBySessionAndUser(Long sessionId, Long userId) {
    return selectOne(
        new LambdaQueryWrapper<LabCheckInTarget>()
            .eq(LabCheckInTarget::getSessionId, sessionId)
            .eq(LabCheckInTarget::getUserId, userId)
            .last("LIMIT 1"));
  }

  default int deleteBySessionId(Long sessionId) {
    return delete(new LambdaQueryWrapper<LabCheckInTarget>().eq(LabCheckInTarget::getSessionId, sessionId));
  }
}
