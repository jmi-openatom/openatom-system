package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAutoScheduleInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewSession;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewerOptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewScheduleVO;
import java.util.List;

public interface InterviewSessionService {
  Result<ResponseInterviewScheduleVO> autoSchedule(RequestAutoScheduleInterviewDTO request);
  Result<List<InterviewSession>> list(Integer campaignId);
  Result<ResponseInterviewScheduleVO> detail(Integer sessionId);
  Result<String> publish(Integer sessionId);
  Result<List<ResponseInterviewerOptionVO>> interviewerOptions(String keyword);
}
