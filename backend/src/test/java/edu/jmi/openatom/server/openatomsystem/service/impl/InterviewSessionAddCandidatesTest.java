package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.jmi.openatom.server.openatomsystem.dto.RequestAddInterviewCandidatesDTO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterviewSessionAddCandidatesTest {
  private final InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
  private final InterviewRoomMapper rooms = mock(InterviewRoomMapper.class);
  private final InterviewRoomInterviewerMapper roomPeople = mock(InterviewRoomInterviewerMapper.class);
  private final InterviewMapper interviews = mock(InterviewMapper.class);
  private final InterviewInterviewerMapper people = mock(InterviewInterviewerMapper.class);
  private final MembershipApplicationMapper applications = mock(MembershipApplicationMapper.class);
  private final UserMapper users = mock(UserMapper.class);
  private final NotificationService notifications = mock(NotificationService.class);
  private final MailBroadcastPlanner mail = mock(MailBroadcastPlanner.class);
  private final InterviewSessionServiceImpl service = new InterviewSessionServiceImpl(
      sessions, rooms, roomPeople, interviews, people, applications, users, notifications, mail);
  private final InterviewSession session = InterviewSession.builder().id(3).campaignId(9).name("第一轮")
      .round(1).durationMinutes(30).gapMinutes(10).mode("offline").status("draft")
      .scheduledStartAt(time("14:00")).scheduledEndAt(time("14:30")).build();
  private final MembershipApplication app = application(2);
  private final Interview existing = Interview.builder().id(11).sessionId(3).roomId(7)
      .applicationId(1).queueNumber(6).round(1).status("draft")
      .scheduledStartAt(time("14:00")).scheduledEndAt(time("14:30")).build();

  @BeforeEach
  void prepare() {
    when(sessions.selectByIdForUpdate(3)).thenReturn(session);
    when(rooms.selectBySessionId(3)).thenReturn(List.of(
        InterviewRoom.builder().id(7).sessionId(3).name("甲室").location("A201").build(),
        InterviewRoom.builder().id(8).sessionId(3).name("乙室").location("A202").build()));
    when(roomPeople.selectByRoomIds(anyList())).thenReturn(List.of(
        InterviewRoomInterviewer.builder().roomId(7).interviewerId(101).build(),
        InterviewRoomInterviewer.builder().roomId(8).interviewerId(102).build()));
    when(users.selectBatchIds(anyCollection())).thenReturn(List.of(
        User.builder().id(101).realName("甲考官").userStatus(UserStatus.ACTIVE).build(),
        User.builder().id(102).realName("乙考官").userStatus(UserStatus.ACTIVE).build()));
    when(applications.selectBatchIds(anyCollection())).thenReturn(List.of(app));
    when(interviews.selectBySessionId(3)).thenReturn(List.of(existing));
    when(interviews.selectList(any())).thenReturn(List.of());
    when(interviews.selectByApplicationIds(anyList())).thenReturn(List.of());
    doAnswer(invocation -> { ((Interview) invocation.getArgument(0)).setId(20); return 1; })
        .when(interviews).insert(any(Interview.class));
  }

  @Test
  void previewChoosesEarliestRoomAndDoesNotWrite() {
    var result = service.addCandidates(3, request(true, null));
    assertEquals(0, result.getCode());
    var added = result.getData().getAssignments().getFirst();
    assertEquals(8, added.getRoomId());
    assertEquals(7, added.getQueueNumber());
    assertEquals(time("14:00"), added.getScheduledStartAt());
    assertEquals("候选人2", added.getApplicantName());
    verify(interviews, never()).insert(any(Interview.class));
    verify(sessions, never()).updateById(any(InterviewSession.class));
    verifyNoInteractions(notifications, mail);
  }

  @Test
  void savingDraftExtendsPastOldEndAndPreservesExistingSchedule() {
    assertEquals(0, service.addCandidates(3, request(false, 7)).getCode());
    verify(interviews).insert(argThat((Interview item) -> item.getRoomId() == 7
        && item.getQueueNumber() == 7 && "draft".equals(item.getStatus())
        && time("14:40").equals(item.getScheduledStartAt()) && time("15:10").equals(item.getScheduledEndAt())
        && "A201".equals(item.getLocation())));
    verify(people).insert(InterviewInterviewer.builder().interviewId(20).interviewerId(101).build());
    verify(sessions).updateById(argThat((InterviewSession item) -> time("15:10").equals(item.getScheduledEndAt())));
    verify(interviews, never()).updateById(any(Interview.class));
    verify(applications, never()).updateById(any(MembershipApplication.class));
    assertEquals(time("14:00"), existing.getScheduledStartAt());
    verifyNoInteractions(notifications, mail);
  }

  @Test
  void publishedAdditionIsPendingAndOnlyNotifiesNewCandidate() {
    session.setStatus("published");
    app.setUserId(200);
    assertEquals(0, service.addCandidates(3, request(false, 7)).getCode());
    verify(interviews).insert(argThat((Interview item) -> "pending".equals(item.getStatus())));
    verify(applications).updateById(argThat((MembershipApplication item) -> item.getId() == 2
        && "interview_scheduled".equals(item.getStatus())));
    verify(notifications).create(argThat(item -> item.getReceiverUserIds().equals(List.of(200))));
    verify(mail).enqueueUserMail(eq("broadcast_interview_20"), eq(200), eq("interview"), anyString(), anyString());
    verifyNoMoreInteractions(notifications, mail);
    verify(interviews, never()).updateById(any(Interview.class));
  }

  @Test
  void rejectsAlreadyScheduledCandidateEvenWhenApplicationIsStillPreScreenPassed() {
    when(interviews.selectByApplicationIds(anyList())).thenReturn(List.of(Interview.builder()
        .applicationId(2).round(1).sessionId(99).build()));
    assertEquals(409, service.addCandidates(3, request(false, 7)).getCode());
    verify(interviews, never()).insert(any(Interview.class));
  }

  @Test
  void rejectsOtherCampaignInvalidStatusAndForeignRoom() {
    app.setCampaignId(99);
    assertEquals(400, service.addCandidates(3, request(false, 7)).getCode());
    app.setCampaignId(9);
    app.setStatus("rejected");
    assertEquals(422, service.addCandidates(3, request(false, 7)).getCode());
    app.setStatus("pre_screen_passed");
    assertEquals(400, service.addCandidates(3, request(false, 99)).getCode());
    verify(interviews, never()).insert(any(Interview.class));
  }

  @Test
  void rejectsCompletedSession() {
    session.setStatus("completed");
    assertEquals(422, service.addCandidates(3, request(false, 7)).getCode());
    verify(interviews, never()).insert(any(Interview.class));
  }

  @Test
  void duplicateIdsAreRejected() {
    var request = request(false, 7);
    request.setApplicationIds(List.of(2, 2));
    assertEquals(400, service.addCandidates(3, request).getCode());
    verify(interviews, never()).insert(any(Interview.class));
  }

  @Test
  void laterCandidateConflictDoesNotPartiallySaveBatch() {
    when(applications.selectBatchIds(anyCollection())).thenReturn(List.of(app, application(3)));
    when(interviews.selectList(any())).thenReturn(List.of(), List.of(Interview.builder()
        .status("pending").scheduledStartAt(time("15:20")).scheduledEndAt(time("15:50")).build()));
    var request = request(false, 7);
    request.setApplicationIds(List.of(2, 3));
    assertEquals(409, service.addCandidates(3, request).getCode());
    verify(interviews, never()).insert(any(Interview.class));
    verify(sessions, never()).updateById(any(InterviewSession.class));
    verifyNoInteractions(notifications, mail);
  }

  @Test
  void extendingScheduleStillChecksInterviewerConflict() {
    when(people.countOverlapping(eq(101), eq(time("14:40")), eq(time("15:10")))).thenReturn(1L);
    assertEquals(409, service.addCandidates(3, request(false, 7)).getCode());
    verify(interviews, never()).insert(any(Interview.class));
  }

  private RequestAddInterviewCandidatesDTO request(boolean preview, Integer roomId) {
    var request = new RequestAddInterviewCandidatesDTO();
    request.setApplicationIds(List.of(2));
    request.setRoomId(roomId);
    request.setPreviewOnly(preview);
    return request;
  }

  private static MembershipApplication application(int id) {
    return MembershipApplication.builder().id(id).campaignId(9).status("pre_screen_passed")
        .profile("{\"name\":\"候选人" + id + "\"}").build();
  }

  private static Timestamp time(String value) { return Timestamp.valueOf("2026-10-15 " + value + ":00"); }
}
