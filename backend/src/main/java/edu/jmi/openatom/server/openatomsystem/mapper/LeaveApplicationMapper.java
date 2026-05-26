package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.LeaveApplication;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LeaveApplicationMapper extends BaseMapper<LeaveApplication> {
  default List<LeaveApplication> selectAllOrdered(String status) {
    LambdaQueryWrapper<LeaveApplication> wrapper =
        new LambdaQueryWrapper<LeaveApplication>().orderByDesc(LeaveApplication::getId);
    if (status != null && !status.isBlank()) {
      wrapper.eq(LeaveApplication::getStatus, status.trim());
    }
    return selectList(wrapper);
  }

  default List<LeaveApplication> selectByUserOrdered(Integer userId) {
    return selectList(
        new LambdaQueryWrapper<LeaveApplication>()
            .eq(LeaveApplication::getUserId, userId)
            .orderByDesc(LeaveApplication::getId));
  }

  default LeaveApplication selectByAttachmentFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) return null;
    return selectOne(
        new LambdaQueryWrapper<LeaveApplication>()
            .like(LeaveApplication::getAttachments, fileName.trim())
            .last("LIMIT 1"));
  }
}
