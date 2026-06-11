package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInSession;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInSessionMapper extends BaseMapper<CheckInSession> {
  default CheckInSession selectByToken(String token) {
    return selectOne(
        new LambdaQueryWrapper<CheckInSession>()
            .eq(CheckInSession::getToken, token)
            .last("LIMIT 1"));
  }

  default List<CheckInSession> selectByClubAndStatus(Integer clubId, String status) {
    return selectList(
        new LambdaQueryWrapper<CheckInSession>()
            .eq(CheckInSession::getClubId, clubId)
            .eq(status != null && !status.isBlank(), CheckInSession::getStatus, status)
            .orderByDesc(CheckInSession::getId));
  }

  default CheckInSession selectEveningByScheduleAndDate(Integer scheduleId, Date attendanceDate) {
    if (scheduleId == null || attendanceDate == null) return null;
    return selectOne(
        new LambdaQueryWrapper<CheckInSession>()
            .eq(CheckInSession::getScheduleId, scheduleId)
            .eq(CheckInSession::getAttendanceDate, attendanceDate)
            .eq(CheckInSession::getSessionType, "evening_study")
            .last("LIMIT 1"));
  }

  default List<CheckInSession> selectEveningByClubAndDate(Integer clubId, Date attendanceDate) {
    return selectList(
        new LambdaQueryWrapper<CheckInSession>()
            .eq(CheckInSession::getClubId, clubId)
            .eq(CheckInSession::getSessionType, "evening_study")
            .eq(attendanceDate != null, CheckInSession::getAttendanceDate, attendanceDate)
            .orderByAsc(CheckInSession::getStartAt)
            .orderByAsc(CheckInSession::getId));
  }

  default List<CheckInSession> selectEveningOverlaps(Integer clubId, Timestamp startAt, Timestamp endAt) {
    LambdaQueryWrapper<CheckInSession> wrapper =
        new LambdaQueryWrapper<CheckInSession>()
            .eq(CheckInSession::getClubId, clubId)
            .eq(CheckInSession::getSessionType, "evening_study");
    if (endAt != null) {
      wrapper.and(query -> query.isNull(CheckInSession::getStartAt).or().lt(CheckInSession::getStartAt, endAt));
    }
    if (startAt != null) {
      wrapper.and(query -> query.isNull(CheckInSession::getEndAt).or().gt(CheckInSession::getEndAt, startAt));
    }
    return selectList(wrapper.orderByAsc(CheckInSession::getStartAt).orderByAsc(CheckInSession::getId));
  }
}
