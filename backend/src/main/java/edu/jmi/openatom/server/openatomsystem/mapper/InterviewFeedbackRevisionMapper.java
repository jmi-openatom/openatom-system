package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedbackRevision;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewFeedbackRevisionMapper extends BaseMapper<InterviewFeedbackRevision> {
  default List<InterviewFeedbackRevision> selectByInterviewId(Integer interviewId) {
    return selectList(new LambdaQueryWrapper<InterviewFeedbackRevision>()
        .eq(InterviewFeedbackRevision::getInterviewId, interviewId)
        .orderByDesc(InterviewFeedbackRevision::getId));
  }
}
