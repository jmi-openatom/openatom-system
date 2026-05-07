package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedback;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewFeedbackMapper extends BaseMapper<InterviewFeedback> {

  /** 按面试ID查反馈（ordered by id desc） */
  default List<InterviewFeedback> selectByInterviewId(Integer interviewId) {
    return selectList(
        new LambdaQueryWrapper<InterviewFeedback>()
            .eq(InterviewFeedback::getInterviewId, interviewId)
            .orderByDesc(InterviewFeedback::getId));
  }
}
