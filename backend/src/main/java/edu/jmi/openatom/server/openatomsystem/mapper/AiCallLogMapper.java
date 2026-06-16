package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.AiCallLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {
  default List<AiCallLog> selectRecent(String scene, Integer limit) {
    LambdaQueryWrapper<AiCallLog> wrapper =
        new LambdaQueryWrapper<AiCallLog>().orderByDesc(AiCallLog::getId);
    if (scene != null && !scene.isBlank()) {
      wrapper.eq(AiCallLog::getScene, scene);
    }
    wrapper.last("LIMIT " + Math.max(1, Math.min(limit == null ? 100 : limit, 500)));
    return selectList(wrapper);
  }
}
