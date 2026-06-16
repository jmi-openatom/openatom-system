package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.GeneratedDocument;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GeneratedDocumentMapper extends BaseMapper<GeneratedDocument> {
  default List<GeneratedDocument> selectBySessionId(Long sessionId) {
    return selectList(
        new LambdaQueryWrapper<GeneratedDocument>()
            .eq(GeneratedDocument::getSessionId, sessionId)
            .orderByDesc(GeneratedDocument::getId));
  }
}
