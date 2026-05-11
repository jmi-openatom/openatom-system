package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedback;
import java.util.List;

/**
 * 面试管理服务接口
 *
 * <p>定义面试的列表查询, 创建, 查看详情, 更新, 确认, 提交反馈, 完成以及查看反馈列表等业务操作
 */
public interface InterviewService {
  ApiResponse<List<Interview>> list(
      Integer campaignId, Integer applicationId, Integer interviewerId, String status);

  ApiResponse<String> create(RequestCreateInterviewDTO request);

  ApiResponse<Interview> detail(Integer interviewId);

  ApiResponse<String> update(Integer interviewId, RequestUpdateInterviewDTO request);

  ApiResponse<String> confirm(Integer interviewId);

  ApiResponse<String> feedback(Integer interviewId, RequestInterviewFeedbackDTO request);

  ApiResponse<String> complete(Integer interviewId);

  ApiResponse<List<InterviewFeedback>> getFeedbacks(Integer interviewId);
}
