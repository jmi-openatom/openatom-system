package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewInterviewer;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewInterviewerMapper extends BaseMapper<InterviewInterviewer> {

  /** 按面试官ID查 */
  default List<InterviewInterviewer> selectByInterviewerId(Integer interviewerId) {
    return selectList(
        new LambdaQueryWrapper<InterviewInterviewer>()
            .eq(InterviewInterviewer::getInterviewerId, interviewerId));
  }

  /** 按面试ID删除 */
  default int deleteByInterviewId(Integer interviewId) {
    return delete(
        new LambdaQueryWrapper<InterviewInterviewer>()
            .eq(InterviewInterviewer::getInterviewId, interviewId));
  }
}
