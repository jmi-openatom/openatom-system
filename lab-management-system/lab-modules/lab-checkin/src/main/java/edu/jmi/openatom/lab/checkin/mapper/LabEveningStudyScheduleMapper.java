package edu.jmi.openatom.lab.checkin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.checkin.entity.LabEveningStudySchedule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabEveningStudyScheduleMapper extends BaseMapper<LabEveningStudySchedule> {
  default List<LabEveningStudySchedule> selectOrdered() {
    return selectList(
        new LambdaQueryWrapper<LabEveningStudySchedule>()
            .orderByDesc(LabEveningStudySchedule::getEnabled)
            .orderByAsc(LabEveningStudySchedule::getStartTime)
            .orderByAsc(LabEveningStudySchedule::getId));
  }

  default List<LabEveningStudySchedule> selectEnabled() {
    return selectList(
        new LambdaQueryWrapper<LabEveningStudySchedule>()
            .eq(LabEveningStudySchedule::getEnabled, true)
            .orderByAsc(LabEveningStudySchedule::getStartTime)
            .orderByAsc(LabEveningStudySchedule::getId));
  }

  default LabEveningStudySchedule selectByGroupId(Long groupId) {
    if (groupId == null) {
      return null;
    }
    return selectOne(
        new LambdaQueryWrapper<LabEveningStudySchedule>()
            .eq(LabEveningStudySchedule::getGroupId, groupId)
            .last("LIMIT 1"));
  }
}
