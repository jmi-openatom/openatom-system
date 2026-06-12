package edu.jmi.openatom.lab.checkin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabCheckInGroupMapper extends BaseMapper<LabCheckInGroup> {
  default List<LabCheckInGroup> selectAllOrdered() {
    return selectList(new LambdaQueryWrapper<LabCheckInGroup>().orderByDesc(LabCheckInGroup::getId));
  }
}
