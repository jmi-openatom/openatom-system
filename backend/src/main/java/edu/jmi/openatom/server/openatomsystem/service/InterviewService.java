package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import java.util.List;

public interface InterviewService {
  ApiResponse<List<Interview>> list(
      Integer campaignId, Integer applicationId, Integer interviewerId, String status);

  ApiResponse<String> create(RequestCreateInterviewDTO request);

  ApiResponse<Interview> detail(Integer interviewId);

  ApiResponse<String> update(Integer interviewId, RequestUpdateInterviewDTO request);

  ApiResponse<String> confirm(Integer interviewId);

  ApiResponse<String> feedback(Integer interviewId, RequestInterviewFeedbackDTO request);

  ApiResponse<String> complete(Integer interviewId);
}
