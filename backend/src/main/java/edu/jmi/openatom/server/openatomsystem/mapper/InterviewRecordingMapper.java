package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRecording;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewRecordingMapper extends BaseMapper<InterviewRecording> {
  default List<InterviewRecording> selectByInterviewId(Integer interviewId) {
    return selectList(new LambdaQueryWrapper<InterviewRecording>()
        .eq(InterviewRecording::getInterviewId, interviewId)
        .orderByDesc(InterviewRecording::getId));
  }
}
