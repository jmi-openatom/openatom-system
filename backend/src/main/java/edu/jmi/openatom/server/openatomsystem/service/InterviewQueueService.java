package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewQueueVO;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewQueueOperation;
import java.util.List;

public interface InterviewQueueService {
  Result<ResponseInterviewQueueVO> detail(Integer sessionId);
  Result<ResponseInterviewQueueVO> callScreen(Integer sessionId);
  Result<String> checkIn(Integer interviewId);
  Result<String> undoCheckIn(Integer interviewId);
  Result<ResponseInterviewQueueVO.Candidate> callNext(Integer roomId);
  Result<ResponseInterviewQueueVO.Candidate> callAgain(Integer roomId);
  Result<String> markNoShow(Integer interviewId);
  Result<String> restoreWaiting(Integer interviewId);
  Result<String> moveRoom(Integer interviewId, Integer targetRoomId);
  Result<String> recoverRoom(Integer roomId);
  Result<String> completeSession(Integer sessionId);
  Result<String> reopenSession(Integer sessionId);
  Result<List<InterviewQueueOperation>> operations(Integer sessionId);
  Result<byte[]> exportEvaluationSummary(Integer sessionId);
}
