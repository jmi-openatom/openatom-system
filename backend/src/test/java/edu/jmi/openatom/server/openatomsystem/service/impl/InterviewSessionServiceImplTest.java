package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.jmi.openatom.server.openatomsystem.dto.RequestAutoScheduleInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewInterviewerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewRoomInterviewerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewRoomMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewSessionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterviewSessionServiceImplTest {

  @Test
  void previewBalancesCandidatesAcrossFixedRooms() {
    MembershipApplicationMapper applicationMapper = mock(MembershipApplicationMapper.class);
    UserMapper userMapper = mock(UserMapper.class);
    InterviewInterviewerMapper interviewerMapper = mock(InterviewInterviewerMapper.class);
    InterviewMapper interviewMapper = mock(InterviewMapper.class);
    List<MembershipApplication> applications = List.of(1, 2, 3, 4, 5).stream()
        .map(id -> MembershipApplication.builder().id(id).campaignId(9)
            .status("pre_screen_passed").build()).toList();
    when(applicationMapper.selectBatchIds(anyCollection())).thenReturn(applications);
    when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
        User.builder().id(101).realName("甲面试官").userStatus(UserStatus.ACTIVE).build(),
        User.builder().id(102).realName("乙面试官").userStatus(UserStatus.ACTIVE).build()));
    when(interviewMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    InterviewSessionServiceImpl service = new InterviewSessionServiceImpl(
        mock(InterviewSessionMapper.class), mock(InterviewRoomMapper.class),
        mock(InterviewRoomInterviewerMapper.class), interviewMapper, interviewerMapper,
        applicationMapper, userMapper, null, null);

    RequestAutoScheduleInterviewDTO request = request();
    var result = service.autoSchedule(request);

    assertEquals(0, result.getCode());
    assertEquals(List.of(3, 2), result.getData().getRooms().stream()
        .map(room -> room.getAssignedCount()).toList());
    assertEquals(5, result.getData().getAssignments().size());
  }

  private RequestAutoScheduleInterviewDTO request() {
    RequestAutoScheduleInterviewDTO first = new RequestAutoScheduleInterviewDTO();
    first.setCampaignId(9);
    first.setName("第一轮面试");
    first.setRound(1);
    first.setScheduledStartAt("2026-09-10T14:00:00");
    first.setScheduledEndAt("2026-09-10T17:00:00");
    first.setDurationMinutes(30);
    first.setGapMinutes(10);
    first.setMode("offline");
    first.setApplicationIds(List.of(1, 2, 3, 4, 5));
    first.setRooms(List.of(room("第一面试间", 101), room("第二面试间", 102)));
    first.setPreviewOnly(true);
    return first;
  }

  private RequestAutoScheduleInterviewDTO.Room room(String name, Integer interviewerId) {
    RequestAutoScheduleInterviewDTO.Room room = new RequestAutoScheduleInterviewDTO.Room();
    room.setName(name);
    room.setLocation(name);
    room.setInterviewerIds(List.of(interviewerId));
    return room;
  }
}
