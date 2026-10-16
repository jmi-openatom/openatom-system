package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewRoomDTO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InterviewRoomEditTest {
  private final InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
  private final InterviewRoomMapper rooms = mock(InterviewRoomMapper.class);
  private final InterviewMapper interviews = mock(InterviewMapper.class);
  private final InterviewQueueStateMapper queues = mock(InterviewQueueStateMapper.class);
  private final InterviewQueueOperationMapper operations = mock(InterviewQueueOperationMapper.class);
  private final InterviewRoomInterviewerMapper roomPeople = mock(InterviewRoomInterviewerMapper.class);
  private final InterviewInterviewerMapper people = mock(InterviewInterviewerMapper.class);
  private final InterviewFeedbackMapper feedback = mock(InterviewFeedbackMapper.class);
  private final UserMapper users = mock(UserMapper.class);
  private final InterviewQueueServiceImpl service = new InterviewQueueServiceImpl(sessions, rooms,
      interviews, queues, operations, roomPeople, people, feedback, mock(MembershipApplicationMapper.class), users);
  private StpLogic originalLogic;

  @BeforeEach
  void prepare() {
    originalLogic = StpUtil.getStpLogic();
    StpLogic logic = mock(StpLogic.class);
    when(logic.getLoginType()).thenReturn("login");
    StpUtil.setStpLogic(logic);
    when(rooms.selectByIdForUpdate(7)).thenReturn(InterviewRoom.builder()
        .id(7).sessionId(3).name("旧名称").location("旧地点").build());
    when(rooms.selectById(7)).thenAnswer(ignored -> rooms.selectByIdForUpdate(7));
    when(roomPeople.selectByRoomId(7)).thenReturn(List.of(
        InterviewRoomInterviewer.builder().roomId(7).interviewerId(101).build()));
  }

  @AfterEach
  void restoreAuthentication() { StpUtil.setStpLogic(originalLogic); }

  @ParameterizedTest
  @ValueSource(strings = {"draft", "published", "completed"})
  void nameAndLocationCanBeChangedInEverySessionState(String status) {
    when(sessions.selectByIdForUpdate(3)).thenReturn(InterviewSession.builder().id(3).status(status).build());

    assertEquals(0, service.updateRoom(7, request(101)).getCode());

    verify(rooms).updateById(argThat((InterviewRoom r) -> "新名称".equals(r.getName()) && "A201".equals(r.getLocation())));
    verify(interviews).updateLocationByRoomId(3, 7, "A201");
    verify(roomPeople, never()).deleteByRoomId(anyInt());
    verify(operations).insert(argThat((InterviewQueueOperation op) -> "update_room".equals(op.getAction())));
  }

  @Test
  void changingGroupPreservesSubmittedEvaluationsAndCompletedInterviewAssignments() {
    prepareGroupChange();
    Interview pending = interview(21, "confirmed"), completed = interview(22, "completed"), allSubmitted = interview(23, "confirmed");
    when(interviews.selectBySessionId(3)).thenReturn(List.of(pending, completed, allSubmitted));
    when(feedback.selectByInterviewId(21)).thenReturn(List.of(
        InterviewFeedback.builder().interviewerId(101).status("submitted").build()));
    when(feedback.selectByInterviewId(23)).thenReturn(List.of(
        InterviewFeedback.builder().interviewerId(102).status("submitted").build()));
    InterviewQueueState state = InterviewQueueState.builder().interviewId(23).status("pending_feedback").build();
    when(queues.selectByInterviewId(23)).thenReturn(state);

    assertEquals(0, service.updateRoom(7, request(102)).getCode());

    verify(roomPeople).deleteByRoomId(7);
    verify(roomPeople).insert(argThat((InterviewRoomInterviewer link) -> link.getInterviewerId() == 102));
    verify(people).insert(InterviewInterviewer.builder().interviewId(21).interviewerId(101).build());
    verify(people).insert(InterviewInterviewer.builder().interviewId(21).interviewerId(102).build());
    verify(people, never()).deleteByInterviewId(22);
    assertEquals("confirmed", pending.getStatus());
    assertEquals("completed", allSubmitted.getStatus());
    assertEquals("completed", state.getStatus());
  }

  @Test
  void rejectsInterviewerAlreadyAssignedToAnotherRoomWithoutWriting() {
    prepareGroupChange();
    when(rooms.selectBySessionId(3)).thenReturn(List.of(InterviewRoom.builder().id(8).build()));
    when(roomPeople.selectByRoomIds(List.of(8))).thenReturn(List.of(
        InterviewRoomInterviewer.builder().roomId(8).interviewerId(102).build()));

    assertEquals(409, service.updateRoom(7, request(102)).getCode());

    verify(rooms, never()).updateById(any(InterviewRoom.class));
    verifyNoInteractions(operations);
  }

  @Test
  void rejectsOverlappingInterviewWithoutWriting() {
    prepareGroupChange();
    Interview interview = interview(21, "pending");
    when(interviews.selectBySessionId(3)).thenReturn(List.of(interview));
    when(people.countOverlappingOutsideRoom(102, 7, interview.getScheduledStartAt(), interview.getScheduledEndAt())).thenReturn(1L);

    assertEquals(409, service.updateRoom(7, request(102)).getCode());

    verify(rooms, never()).updateById(any(InterviewRoom.class));
    verifyNoInteractions(operations);
  }

  @Test
  void rejectsInactiveOrMissingInterviewer() {
    prepareGroupChange();
    when(users.selectBatchIds(List.of(102))).thenReturn(List.of());
    assertEquals(400, service.updateRoom(7, request(102)).getCode());
    verify(rooms, never()).updateById(any(InterviewRoom.class));
  }

  private void prepareGroupChange() {
    when(sessions.selectByIdForUpdate(3)).thenReturn(InterviewSession.builder().id(3).status("published").build());
    when(users.selectBatchIds(List.of(102))).thenReturn(List.of(User.builder().id(102).userStatus(UserStatus.ACTIVE).build()));
  }

  private RequestUpdateInterviewRoomDTO request(int interviewerId) {
    RequestUpdateInterviewRoomDTO request = new RequestUpdateInterviewRoomDTO();
    request.setName(" 新名称 "); request.setLocation(" A201 "); request.setInterviewerIds(List.of(interviewerId));
    return request;
  }

  private Interview interview(int id, String status) {
    return Interview.builder().id(id).roomId(7).sessionId(3).status(status)
        .scheduledStartAt(Timestamp.from(Instant.parse("2026-10-15T02:00:00Z")))
        .scheduledEndAt(Timestamp.from(Instant.parse("2026-10-15T02:10:00Z"))).build();
  }
}
