package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroupMember;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInGroupMemberMapper extends BaseMapper<CheckInGroupMember> {
  default List<CheckInGroupMember> selectByGroupId(Integer groupId) {
    return selectList(new LambdaQueryWrapper<CheckInGroupMember>().eq(CheckInGroupMember::getGroupId, groupId));
  }

  default int deleteByGroupId(Integer groupId) {
    return delete(new LambdaQueryWrapper<CheckInGroupMember>().eq(CheckInGroupMember::getGroupId, groupId));
  }

  default int deleteByGroupIdAndUserId(Integer groupId, Integer userId) {
    return delete(
        new LambdaQueryWrapper<CheckInGroupMember>()
            .eq(CheckInGroupMember::getGroupId, groupId)
            .eq(CheckInGroupMember::getUserId, userId));
  }
}
