package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRoomInterviewer;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewRoomInterviewerMapper extends BaseMapper<InterviewRoomInterviewer> {
  default List<InterviewRoomInterviewer> selectByRoomId(Integer roomId) {
    return selectList(new LambdaQueryWrapper<InterviewRoomInterviewer>()
        .eq(InterviewRoomInterviewer::getRoomId, roomId));
  }

  default List<InterviewRoomInterviewer> selectByRoomIds(List<Integer> roomIds) {
    if (roomIds == null || roomIds.isEmpty()) return List.of();
    return selectList(new LambdaQueryWrapper<InterviewRoomInterviewer>()
        .in(InterviewRoomInterviewer::getRoomId, roomIds));
  }
}
