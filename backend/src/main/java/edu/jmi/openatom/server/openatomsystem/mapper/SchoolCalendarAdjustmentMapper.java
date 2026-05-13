package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.SchoolCalendarAdjustment;
import java.sql.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SchoolCalendarAdjustmentMapper extends BaseMapper<SchoolCalendarAdjustment> {
  default List<SchoolCalendarAdjustment> selectAllOrdered() {
    return selectList(new LambdaQueryWrapper<SchoolCalendarAdjustment>().orderByAsc(SchoolCalendarAdjustment::getCalendarDate));
  }

  default SchoolCalendarAdjustment selectByDate(Date date) {
    return selectOne(new LambdaQueryWrapper<SchoolCalendarAdjustment>().eq(SchoolCalendarAdjustment::getCalendarDate, date));
  }
}
