package edu.jmi.openatom.lab.checkin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabCheckInSessionMapper extends BaseMapper<LabCheckInSession> {
  default LabCheckInSession selectByToken(String token) {
    return selectOne(
        new LambdaQueryWrapper<LabCheckInSession>()
            .eq(LabCheckInSession::getToken, token)
            .last("LIMIT 1"));
  }

  default List<LabCheckInSession> selectByStatus(String status) {
    return selectList(
        new LambdaQueryWrapper<LabCheckInSession>()
            .eq(status != null && !status.isBlank(), LabCheckInSession::getStatus, status)
            .orderByDesc(LabCheckInSession::getId));
  }

  default LabCheckInSession selectEveningByScheduleAndDate(Long scheduleId, LocalDate attendanceDate) {
    if (scheduleId == null || attendanceDate == null) {
      return null;
    }
    return selectOne(
        new LambdaQueryWrapper<LabCheckInSession>()
            .eq(LabCheckInSession::getScheduleId, scheduleId)
            .eq(LabCheckInSession::getAttendanceDate, attendanceDate)
            .eq(LabCheckInSession::getSessionType, "evening_study")
            .last("LIMIT 1"));
  }

  default List<LabCheckInSession> selectEveningByDate(LocalDate attendanceDate) {
    return selectList(
        new LambdaQueryWrapper<LabCheckInSession>()
            .eq(LabCheckInSession::getSessionType, "evening_study")
            .eq(attendanceDate != null, LabCheckInSession::getAttendanceDate, attendanceDate)
            .orderByAsc(LabCheckInSession::getStartAt)
            .orderByAsc(LabCheckInSession::getId));
  }

  default List<LabCheckInSession> selectOpenEveningStarted(LocalDateTime now) {
    return selectList(
        new LambdaQueryWrapper<LabCheckInSession>()
            .eq(LabCheckInSession::getSessionType, "evening_study")
            .eq(LabCheckInSession::getStatus, "open")
            .isNotNull(LabCheckInSession::getStartAt)
            .le(now != null, LabCheckInSession::getStartAt, now)
            .orderByAsc(LabCheckInSession::getStartAt)
            .orderByAsc(LabCheckInSession::getId));
  }
}
