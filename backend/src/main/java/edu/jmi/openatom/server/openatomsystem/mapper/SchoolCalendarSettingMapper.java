package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.SchoolCalendarSetting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SchoolCalendarSettingMapper extends BaseMapper<SchoolCalendarSetting> {
  default SchoolCalendarSetting selectCurrent() {
    return selectOne(new LambdaQueryWrapper<SchoolCalendarSetting>().orderByAsc(SchoolCalendarSetting::getId).last("LIMIT 1"));
  }
}
