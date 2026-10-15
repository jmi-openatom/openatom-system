package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewInterviewer;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewScheduleVO;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterviewScheduleExportTest {
  private final InterviewInterviewerMapper people = mock(InterviewInterviewerMapper.class);
  private final UserMapper users = mock(UserMapper.class);
  private final InterviewSessionServiceImpl service = spy(new InterviewSessionServiceImpl(
      mock(InterviewSessionMapper.class), mock(InterviewRoomMapper.class), mock(InterviewRoomInterviewerMapper.class),
      mock(InterviewMapper.class), people, mock(MembershipApplicationMapper.class), users, null, null));

  @Test
  void exportsCurrentRoomDataBeijingTimesAndActualInterviewersWithSafeCsvEscaping() {
    doReturn(Result.success(ResponseInterviewScheduleVO.builder().sessionName("秋季招新")
        .rooms(List.of(ResponseInterviewScheduleVO.Room.builder().roomId(7).location("A201,东侧").build()))
        .assignments(List.of(ResponseInterviewScheduleVO.Assignment.builder().interviewId(21).queueNumber(1)
            .roomId(7).roomName("新面试间").applicantName("=危险\"姓名\n下一行")
            .scheduledStartAt(Timestamp.from(Instant.parse("2026-10-15T02:00:00Z")))
            .scheduledEndAt(Timestamp.from(Instant.parse("2026-10-15T02:10:00Z"))).build())).build()))
        .when(service).detail(3);
    when(people.selectByInterviewId(21)).thenReturn(List.of(InterviewInterviewer.builder().interviewId(21).interviewerId(101).build()));
    when(users.selectBatchIds(anyCollection())).thenReturn(List.of(User.builder().id(101).realName("张老师").build()));

    var result = service.exportSchedule(3);

    assertEquals(0, result.getCode());
    String csv = new String(result.getData(), StandardCharsets.UTF_8);
    assertTrue(csv.startsWith("\uFEFF场次,编号,候选人"));
    assertTrue(csv.contains("\"2026-10-15 10:00\",\"2026-10-15 10:10\""));
    assertTrue(csv.contains("\"A201,东侧\""));
    assertTrue(csv.contains("\"'=危险\"\"姓名\n下一行\""));
    assertTrue(csv.contains("\"新面试间\""));
    assertTrue(csv.endsWith("\"张老师\"\r\n"));
  }

  @Test
  void missingSessionDoesNotCreateAnEmptyDownload() {
    doReturn(Result.error(404, "面试场次不存在")).when(service).detail(3);
    var result = service.exportSchedule(3);
    assertEquals(404, result.getCode());
    assertNull(result.getData());
    verifyNoInteractions(people, users);
  }
}
