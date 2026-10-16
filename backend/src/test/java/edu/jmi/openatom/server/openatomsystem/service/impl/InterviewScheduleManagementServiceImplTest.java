package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRescheduleInterviewSessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewRoomDTO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class InterviewScheduleManagementServiceImplTest {
  private final InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
  private final InterviewRoomMapper rooms = mock(InterviewRoomMapper.class);
  private final InterviewRoomInterviewerMapper roomPeople = mock(InterviewRoomInterviewerMapper.class);
  private final InterviewMapper interviews = mock(InterviewMapper.class);
  private final InterviewInterviewerMapper people = mock(InterviewInterviewerMapper.class);
  private final InterviewQueueStateMapper queue = mock(InterviewQueueStateMapper.class);
  private final InterviewFeedbackMapper feedback = mock(InterviewFeedbackMapper.class);
  private final MembershipApplicationMapper apps = mock(MembershipApplicationMapper.class);
  private final UserMapper users = mock(UserMapper.class);
  private final NotificationService notifications = mock(NotificationService.class);
  private final MailBroadcastPlanner mail = mock(MailBroadcastPlanner.class);
  private final InterviewScheduleManagementServiceImpl service = new InterviewScheduleManagementServiceImpl(
      sessions, rooms, roomPeople, interviews, people, queue, feedback, apps, users, notifications, mail);
  private final InterviewSession session = InterviewSession.builder().id(3).campaignId(9).name("第一轮")
      .round(1).durationMinutes(30).gapMinutes(10).mode("offline").status("draft")
      .scheduledStartAt(time("14:00")).scheduledEndAt(time("17:10")).build();
  private final List<InterviewRoom> roomList = List.of(
      InterviewRoom.builder().id(7).sessionId(3).name("甲室").location("A201").sortOrder(0).build(),
      InterviewRoom.builder().id(8).sessionId(3).name("乙室").location("A202").sortOrder(1).build());
  private final List<Interview> interviewList = IntStream.rangeClosed(1, 5).mapToObj(id -> Interview.builder()
      .id(id + 10).applicationId(id).sessionId(3).roomId(7).queueNumber(id).round(1).status("draft")
      .scheduledStartAt(Timestamp.from(time("14:00").toInstant().plusSeconds((id - 1) * 2400L)))
      .scheduledEndAt(Timestamp.from(time("14:30").toInstant().plusSeconds((id - 1) * 2400L)))
      .location("A201").build()).toList();

  @BeforeEach
  void prepare() {
    when(sessions.selectByIdForUpdate(3)).thenReturn(session);
    when(rooms.selectBySessionId(3)).thenReturn(roomList);
    roomList.forEach(room -> when(rooms.selectByIdForUpdate(room.getId())).thenReturn(room));
    when(roomPeople.selectByRoomIds(anyList())).thenReturn(List.of(
        InterviewRoomInterviewer.builder().roomId(7).interviewerId(101).build(),
        InterviewRoomInterviewer.builder().roomId(8).interviewerId(102).build()));
    when(interviews.selectBySessionIdForUpdate(3)).thenReturn(interviewList);
    // Existing allocations in this session must not conflict with their own replacement schedule.
    when(interviews.selectByApplicationIds(anyList())).thenReturn(interviewList);
    when(apps.selectBatchIds(anyCollection())).thenReturn(IntStream.rangeClosed(1, 5).mapToObj(id ->
        MembershipApplication.builder().id(id).campaignId(9).status("pre_screen_passed")
            .profile("{\"name\":\"候选人" + id + "\"}").build()).toList());
    when(users.selectBatchIds(anyCollection())).thenAnswer(invocation -> {
      java.util.Collection<Integer> ids = invocation.getArgument(0);
      return ids.stream().map(id -> User.builder().id(id).realName("用户" + id).userStatus(UserStatus.ACTIVE).build()).toList();
    });
    when(rooms.insert(any(InterviewRoom.class))).thenAnswer(invocation -> { ((InterviewRoom) invocation.getArgument(0)).setId(9); return 1; });
  }

  @AfterEach
  void cleanup() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void addsEmptyRoomWithoutChangingCandidates() {
    assertEquals(9, service.addRoom(3, roomRequest()).getData());
    verify(rooms).insert(argThat((InterviewRoom room) -> room.getSessionId() == 3 && room.getSortOrder() == 2
        && "丙室".equals(room.getName()) && "A203".equals(room.getLocation())));
    verify(roomPeople).insert(InterviewRoomInterviewer.builder().roomId(9).interviewerId(103).build());
    verifyNoInteractions(interviews, notifications, mail);
  }

  @Test
  void rejectsDuplicateRoomAndInterviewerWithoutWriting() {
    var request = roomRequest();
    request.setName("甲室");
    assertEquals(409, service.addRoom(3, request).getCode());
    request.setName("丙室"); request.setInterviewerIds(List.of(101));
    assertEquals(409, service.addRoom(3, request).getCode());
    verify(rooms, never()).insert(any(InterviewRoom.class));
  }

  @Test
  void rejectsMissingOrInactiveInterviewer() {
    when(users.selectBatchIds(anyCollection())).thenReturn(List.of());
    assertEquals(400, service.addRoom(3, roomRequest()).getCode());
    when(users.selectBatchIds(anyCollection())).thenReturn(List.of(User.builder().id(103).build()));
    assertEquals(400, service.addRoom(3, roomRequest()).getCode());
    verify(rooms, never()).insert(any(InterviewRoom.class));
  }

  @Test
  void previewsEveryCandidateAndBalancesAcrossAllRoomsWithoutWriting() {
    var result = service.reschedule(3, request(true));
    assertEquals(0, result.getCode());
    assertEquals(5, result.getData().getTotalCandidates());
    assertEquals(List.of(3, 2), result.getData().getRooms().stream().map(room -> room.getAssignedCount()).toList());
    assertEquals(List.of(7, 8, 7, 8, 7), result.getData().getAssignments().stream().map(row -> row.getRoomId()).toList());
    assertEquals(List.of(11, 12, 13, 14, 15), result.getData().getAssignments().stream().map(row -> row.getInterviewId()).toList());
    assertEquals(time("15:50"), result.getData().getScheduledEndAt());
    assertEquals(time("17:10"), session.getScheduledEndAt());
    assertNotNull(result.getData().getScheduleRevision());
    assertNoScheduleWrites();
  }

  @Test
  void savesAllAssignmentsKeepingOriginalIdsAndSynchronizesWaitingQueue() {
    InterviewQueueState waiting = InterviewQueueState.builder().interviewId(12).sessionId(3).roomId(7)
        .status("waiting").checkedInAt(time("13:00")).build();
    when(queue.selectBySessionId(3)).thenReturn(List.of(waiting));
    var preview = service.reschedule(3, request(true)).getData();
    var request = request(false); request.setExpectedRevision(preview.getScheduleRevision());
    assertEquals(0, service.reschedule(3, request).getCode());
    verify(interviews, times(5)).updateById(any(Interview.class));
    verify(interviews, never()).insert(any(Interview.class));
    verify(interviews, never()).deleteById(any(java.io.Serializable.class));
    verify(people).insert(InterviewInterviewer.builder().interviewId(12).interviewerId(102).build());
    assertEquals(8, waiting.getRoomId());
    assertEquals("waiting", waiting.getStatus());
    assertEquals(time("13:00"), waiting.getCheckedInAt());
    verify(queue).updateById(waiting);
    assertEquals(time("15:50"), session.getScheduledEndAt());
    verifyNoInteractions(notifications, mail);
  }

  @Test
  void supportsNewStartDurationAndGapWithoutEndTimeLimit() {
    var request = request(true); request.setScheduledStartAt("2026-10-16T20:00:00");
    request.setDurationMinutes(60); request.setGapMinutes(15);
    var result = service.reschedule(3, request);
    assertEquals(0, result.getCode());
    assertEquals(time("23:30"), result.getData().getScheduledEndAt());
    assertEquals(time("21:15"), result.getData().getAssignments().get(2).getScheduledStartAt());
  }

  @Test
  void rejectsChangedRoomConfigurationAfterPreview() {
    var result = service.reschedule(3, request(true));
    var request = request(false); request.setExpectedRevision(result.getData().getScheduleRevision());
    roomList.get(1).setLocation("A301");
    assertEquals(409, service.reschedule(3, request).getCode());
    assertNoScheduleWrites();
  }

  @Test
  void candidateConflictOutsideSessionPreventsAllWrites() {
    when(interviews.selectByApplicationIds(anyList())).thenReturn(List.of(Interview.builder().id(99).sessionId(4)
        .applicationId(5).status("pending").scheduledStartAt(time("15:20")).scheduledEndAt(time("16:00")).build()));
    assertEquals(409, service.reschedule(3, request(false)).getCode());
    assertNoScheduleWrites();
  }

  @Test
  void interviewerConflictOutsideSessionPreventsAllWrites() {
    when(people.countOverlappingOutsideSession(eq(102), eq(3), any(), any())).thenReturn(1L);
    assertEquals(409, service.reschedule(3, request(false)).getCode());
    assertNoScheduleWrites();
  }

  @Test
  void completedOrCalledOrEvaluatedInterviewsCannotBeOverwritten() {
    interviewList.getFirst().setStatus("completed");
    assertEquals(422, service.reschedule(3, request(false)).getCode());
    interviewList.getFirst().setStatus("draft");
    when(queue.selectBySessionId(3)).thenReturn(List.of(InterviewQueueState.builder().interviewId(11).status("called").build()));
    assertEquals(422, service.reschedule(3, request(false)).getCode());
    when(queue.selectBySessionId(3)).thenReturn(List.of());
    when(feedback.selectByInterviewId(11)).thenReturn(List.of(InterviewFeedback.builder().status("submitted").build()));
    assertEquals(422, service.reschedule(3, request(false)).getCode());
    assertNoScheduleWrites();
    verify(feedback, never()).delete(any());
  }

  @Test
  void publishedChangesNotifyOnlyChangedCandidatesAfterCommit() {
    session.setStatus("published");
    interviewList.forEach(interview -> interview.setStatus("confirmed"));
    when(apps.selectBatchIds(anyCollection())).thenReturn(IntStream.rangeClosed(1, 5).mapToObj(id ->
        MembershipApplication.builder().id(id).userId(id + 200).build()).toList());
    TransactionSynchronizationManager.initSynchronization();
    assertEquals(0, service.reschedule(3, request(false)).getCode());
    // Candidate 1 keeps the same room/time; the other four receive changed arrangements.
    verify(notifications, times(4)).create(any());
    assertEquals("confirmed", interviewList.getFirst().getStatus());
    assertEquals("pending", interviewList.get(1).getStatus());
    verifyNoInteractions(mail);
    var callbacks = TransactionSynchronizationManager.getSynchronizations();
    assertEquals(4, callbacks.size());
    callbacks.forEach(callback -> callback.afterCommit());
    verify(mail, times(4)).enqueueUserMail(startsWith("broadcast_interview_reschedule_"), anyInt(), eq("interview"), anyString(), contains("北京时间"));
  }

  @Test
  void rejectsCompletedSessionAndInvalidInputs() {
    session.setStatus("completed");
    assertEquals(422, service.addRoom(3, roomRequest()).getCode());
    assertEquals(422, service.reschedule(3, request(false)).getCode());
    session.setStatus("draft");
    var request = request(false); request.setScheduledStartAt("not-a-date");
    assertEquals(400, service.reschedule(3, request).getCode());
    assertNoScheduleWrites();
  }

  private void assertNoScheduleWrites() {
    verify(interviews, never()).updateById(any(Interview.class));
    verify(people, never()).deleteByInterviewId(anyInt());
    verify(sessions, never()).updateById(any(InterviewSession.class));
    verify(queue, never()).updateById(any(InterviewQueueState.class));
    verifyNoInteractions(notifications, mail);
  }

  private RequestUpdateInterviewRoomDTO roomRequest() {
    var request = new RequestUpdateInterviewRoomDTO();
    request.setName(" 丙室 "); request.setLocation(" A203 "); request.setInterviewerIds(List.of(103));
    return request;
  }
  private RequestRescheduleInterviewSessionDTO request(boolean preview) {
    var request = new RequestRescheduleInterviewSessionDTO(); request.setPreviewOnly(preview); return request;
  }
  private static Timestamp time(String value) { return Times.parseTimestamp("2026-10-16T" + value + ":00"); }
}
