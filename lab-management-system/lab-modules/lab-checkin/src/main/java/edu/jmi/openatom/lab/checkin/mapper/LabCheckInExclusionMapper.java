package edu.jmi.openatom.lab.checkin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInExclusion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabCheckInExclusionMapper extends BaseMapper<LabCheckInExclusion> {
  default List<LabCheckInExclusion> selectBySessionId(Long sessionId) {
    return selectList(new LambdaQueryWrapper<LabCheckInExclusion>().eq(LabCheckInExclusion::getSessionId, sessionId));
  }

  default int deleteBySessionId(Long sessionId) {
    return delete(new LambdaQueryWrapper<LabCheckInExclusion>().eq(LabCheckInExclusion::getSessionId, sessionId));
  }
}
