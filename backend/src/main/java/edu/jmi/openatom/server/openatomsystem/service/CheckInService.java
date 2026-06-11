package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInRecordStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInScanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInTargetsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateCheckInSessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestEveningStudyScheduleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.LeaveApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInGroupVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInSessionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseEveningStudyScheduleVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseEveningStudyTodayVO;
import java.util.List;

public interface CheckInService {
  Result<List<ResponseCheckInSessionVO>> list(String status);

  Result<List<User>> userOptions(String keyword);

  Result<List<ResponseCheckInGroupVO>> groups();

  Result<Integer> createGroup(RequestCheckInGroupDTO request);

  Result<String> updateGroup(Integer groupId, RequestCheckInGroupDTO request);

  Result<String> deleteGroup(Integer groupId);

  Result<String> removeGroupMember(Integer groupId, Integer userId);

  Result<ResponseCheckInSessionVO> detail(Integer sessionId);

  Result<Integer> create(RequestCreateCheckInSessionDTO request);

  Result<String> update(Integer sessionId, RequestCreateCheckInSessionDTO request);

  Result<String> close(Integer sessionId);

  Result<String> delete(Integer sessionId);

  Result<String> addTargets(Integer sessionId, RequestCheckInTargetsDTO request);

  Result<List<ResponseCheckInRecordVO>> records(Integer sessionId);

  Result<ResponseCheckInRecordVO> updateRecordStatus(
      Integer sessionId, Integer userId, RequestCheckInRecordStatusDTO request);

  Result<ResponseCheckInRecordVO> scan(RequestCheckInScanDTO request);

  Result<List<ResponseEveningStudyScheduleVO>> eveningStudySchedules();

  Result<Integer> createEveningStudySchedule(RequestEveningStudyScheduleDTO request);

  Result<String> updateEveningStudySchedule(Integer scheduleId, RequestEveningStudyScheduleDTO request);

  Result<String> deleteEveningStudySchedule(Integer scheduleId);

  Result<ResponseEveningStudyTodayVO> generateEveningStudySessions(String date);

  Result<ResponseEveningStudyTodayVO> eveningStudyToday(String date);

  void syncApprovedLeave(LeaveApplication application);
}
