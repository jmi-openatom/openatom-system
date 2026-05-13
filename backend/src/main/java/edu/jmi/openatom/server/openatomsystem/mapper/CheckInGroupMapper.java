package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInGroupMapper extends BaseMapper<CheckInGroup> {
  default List<CheckInGroup> selectByClubId(Integer clubId) {
    return selectList(new LambdaQueryWrapper<CheckInGroup>()
        .eq(CheckInGroup::getClubId, clubId)
        .orderByDesc(CheckInGroup::getId));
  }
}
