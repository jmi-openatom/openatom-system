package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
  @Select("SELECT * FROM interview_session WHERE id = #{sessionId} FOR UPDATE")
  InterviewSession selectByIdForUpdate(Integer sessionId);
}
