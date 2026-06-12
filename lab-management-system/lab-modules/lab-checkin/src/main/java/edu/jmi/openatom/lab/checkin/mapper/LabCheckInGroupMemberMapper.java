package edu.jmi.openatom.lab.checkin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInGroupMember;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabCheckInGroupMemberMapper extends BaseMapper<LabCheckInGroupMember> {
  default List<LabCheckInGroupMember> selectByGroupId(Long groupId) {
    return selectList(new LambdaQueryWrapper<LabCheckInGroupMember>().eq(LabCheckInGroupMember::getGroupId, groupId));
  }

  default int deleteByGroupId(Long groupId) {
    return delete(new LambdaQueryWrapper<LabCheckInGroupMember>().eq(LabCheckInGroupMember::getGroupId, groupId));
  }

  default int deleteByGroupIdAndUserId(Long groupId, Long userId) {
    return delete(
        new LambdaQueryWrapper<LabCheckInGroupMember>()
            .eq(LabCheckInGroupMember::getGroupId, groupId)
            .eq(LabCheckInGroupMember::getUserId, userId));
  }
}
