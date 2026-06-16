package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.AiActivitySession;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiActivitySessionMapper extends BaseMapper<AiActivitySession> {
  default List<AiActivitySession> selectByUser(Integer userId) {
    return selectList(
        new LambdaQueryWrapper<AiActivitySession>()
            .eq(AiActivitySession::getUserId, userId)
            .orderByDesc(AiActivitySession::getUpdatedAt)
            .orderByDesc(AiActivitySession::getId));
  }
}
