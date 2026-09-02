package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewQueueState;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewQueueStateMapper extends BaseMapper<InterviewQueueState> {
  default InterviewQueueState selectByInterviewId(Integer interviewId) {
    return selectOne(new LambdaQueryWrapper<InterviewQueueState>()
        .eq(InterviewQueueState::getInterviewId, interviewId).last("LIMIT 1"));
  }

  default List<InterviewQueueState> selectBySessionId(Integer sessionId) {
    return selectList(new LambdaQueryWrapper<InterviewQueueState>()
        .eq(InterviewQueueState::getSessionId, sessionId));
  }

  default List<InterviewQueueState> selectActiveByRoomId(Integer roomId) {
    return selectList(new LambdaQueryWrapper<InterviewQueueState>()
        .eq(InterviewQueueState::getRoomId, roomId)
        .eq(InterviewQueueState::getStatus, "called")
        .orderByDesc(InterviewQueueState::getCalledAt));
  }
}
