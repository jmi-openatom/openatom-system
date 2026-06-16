package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.AiActivityPlan;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiActivityPlanMapper extends BaseMapper<AiActivityPlan> {
  default List<AiActivityPlan> selectBySessionId(Long sessionId) {
    return selectList(
        new LambdaQueryWrapper<AiActivityPlan>()
            .eq(AiActivityPlan::getSessionId, sessionId)
            .orderByDesc(AiActivityPlan::getVersion)
            .orderByDesc(AiActivityPlan::getId));
  }

  default AiActivityPlan selectLatestBySessionId(Long sessionId) {
    return selectOne(
        new LambdaQueryWrapper<AiActivityPlan>()
            .eq(AiActivityPlan::getSessionId, sessionId)
            .orderByDesc(AiActivityPlan::getVersion)
            .orderByDesc(AiActivityPlan::getId)
            .last("LIMIT 1"));
  }

  default Integer nextVersion(Long sessionId) {
    AiActivityPlan latest = selectLatestBySessionId(sessionId);
    return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
  }
}
