package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRoom;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InterviewRoomMapper extends BaseMapper<InterviewRoom> {
  @Select("SELECT * FROM interview_room WHERE id = #{roomId} FOR UPDATE")
  InterviewRoom selectByIdForUpdate(Integer roomId);

  default List<InterviewRoom> selectBySessionId(Integer sessionId) {
    return selectList(new LambdaQueryWrapper<InterviewRoom>()
        .eq(InterviewRoom::getSessionId, sessionId).orderByAsc(InterviewRoom::getSortOrder));
  }
}
