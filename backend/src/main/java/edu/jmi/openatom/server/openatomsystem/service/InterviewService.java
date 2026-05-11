package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedback;
import java.util.List;

/**
 * 面试管理服务接口
 *
 * <p>定义面试的列表查询, 创建, 查看详情, 更新, 确认, 提交反馈, 完成以及查看反馈列表等业务操作
 */
public interface InterviewService {
  Result<List<Interview>> list(
      Integer campaignId, Integer applicationId, Integer interviewerId, String status);

  Result<String> create(RequestCreateInterviewDTO request);

  Result<Interview> detail(Integer interviewId);

  Result<String> update(Integer interviewId, RequestUpdateInterviewDTO request);

  Result<String> confirm(Integer interviewId);

  Result<String> feedback(Integer interviewId, RequestInterviewFeedbackDTO request);

  Result<String> complete(Integer interviewId);

  Result<List<InterviewFeedback>> getFeedbacks(Integer interviewId);
}
