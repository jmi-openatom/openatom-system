package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.AiActivityMessage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiActivityMessageMapper extends BaseMapper<AiActivityMessage> {
  default List<AiActivityMessage> selectBySessionId(Long sessionId) {
    return selectList(
        new LambdaQueryWrapper<AiActivityMessage>()
            .eq(AiActivityMessage::getSessionId, sessionId)
            .orderByAsc(AiActivityMessage::getId));
  }
}
