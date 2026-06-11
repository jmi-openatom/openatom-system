package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.EveningStudySchedule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EveningStudyScheduleMapper extends BaseMapper<EveningStudySchedule> {
  default List<EveningStudySchedule> selectByClubId(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<EveningStudySchedule>()
            .eq(EveningStudySchedule::getClubId, clubId)
            .orderByDesc(EveningStudySchedule::getEnabled)
            .orderByAsc(EveningStudySchedule::getStartTime)
            .orderByAsc(EveningStudySchedule::getId));
  }

  default List<EveningStudySchedule> selectEnabledByClubId(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<EveningStudySchedule>()
            .eq(EveningStudySchedule::getClubId, clubId)
            .eq(EveningStudySchedule::getEnabled, true)
            .orderByAsc(EveningStudySchedule::getStartTime)
            .orderByAsc(EveningStudySchedule::getId));
  }

  default EveningStudySchedule selectByIdAndClubId(Integer id, Integer clubId) {
    if (id == null) return null;
    return selectOne(
        new LambdaQueryWrapper<EveningStudySchedule>()
            .eq(EveningStudySchedule::getId, id)
            .eq(EveningStudySchedule::getClubId, clubId)
            .last("LIMIT 1"));
  }

  default EveningStudySchedule selectByGroupId(Integer groupId) {
    if (groupId == null) return null;
    return selectOne(
        new LambdaQueryWrapper<EveningStudySchedule>()
            .eq(EveningStudySchedule::getGroupId, groupId)
            .last("LIMIT 1"));
  }
}
