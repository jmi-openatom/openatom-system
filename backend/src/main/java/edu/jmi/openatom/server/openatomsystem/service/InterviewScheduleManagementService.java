package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRescheduleInterviewSessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewRoomDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewScheduleVO;

public interface InterviewScheduleManagementService {
  Result<Integer> addRoom(Integer sessionId, RequestUpdateInterviewRoomDTO request);
  Result<ResponseInterviewScheduleVO> reschedule(Integer sessionId, RequestRescheduleInterviewSessionDTO request);
}
