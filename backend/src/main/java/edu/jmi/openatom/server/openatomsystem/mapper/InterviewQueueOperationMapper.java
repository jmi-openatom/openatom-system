package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewQueueOperation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewQueueOperationMapper extends BaseMapper<InterviewQueueOperation> {
  default List<InterviewQueueOperation> selectRecentBySessionId(Integer sessionId) {
    return selectList(new LambdaQueryWrapper<InterviewQueueOperation>()
        .eq(InterviewQueueOperation::getSessionId, sessionId)
        .orderByDesc(InterviewQueueOperation::getId).last("LIMIT 100"));
  }
}
