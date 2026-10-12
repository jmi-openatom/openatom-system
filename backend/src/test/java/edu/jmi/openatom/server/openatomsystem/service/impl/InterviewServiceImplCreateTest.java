package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewInterviewer;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InterviewServiceImplCreateTest {
  private final InterviewMapper interviews = mock(InterviewMapper.class);
  private final InterviewInterviewerMapper interviewers = mock(InterviewInterviewerMapper.class);
  private final MembershipApplicationMapper applications = mock(MembershipApplicationMapper.class);
  private final NotificationService notifications = mock(NotificationService.class);
  private final MailBroadcastPlanner mail = mock(MailBroadcastPlanner.class);
  private final InterviewServiceImpl service = new InterviewServiceImpl(interviews, interviewers,
      mock(InterviewFeedbackMapper.class), mock(InterviewFeedbackRevisionMapper.class),
      mock(InterviewEvaluationTemplateMapper.class), mock(InterviewQueueStateMapper.class),
      mock(InterviewRoomMapper.class), applications, notifications, mail);

  @Test
  void anonymousApplicationCanBeScheduledWithoutNotifications() {
    schedule(null);
    verifyNoInteractions(notifications, mail);
  }

  @Test
  void boundApplicationStillReceivesInterviewNotifications() {
    schedule(201);
    ArgumentCaptor<RequestCreateNotificationDTO> notification =
        ArgumentCaptor.forClass(RequestCreateNotificationDTO.class);
    verify(notifications).create(notification.capture());
    assertEquals(List.of(201), notification.getValue().getReceiverUserIds());
    verify(mail).enqueueUserMail(eq("broadcast_interview_21"), eq(201), eq("interview"),
        eq("面试安排通知"), eq(notification.getValue().getContent()));
  }

  private void schedule(Integer userId) {
    MembershipApplication application = MembershipApplication.builder()
        .id(4).userId(userId).status("pre_screen_passed").build();
    when(applications.selectById(4)).thenReturn(application);
    when(interviews.insert(any(Interview.class))).thenAnswer(invocation -> {
      invocation.<Interview>getArgument(0).setId(21);
      return 1;
    });
    RequestCreateInterviewDTO request = RequestCreateInterviewDTO.builder()
        .applicationId(4).round(1).scheduledStartAt("2026-10-12T14:00:00")
        .scheduledEndAt("2026-10-12T14:30:00").location("第一面试间")
        .mode("offline").interviewerIds(List.of(101)).build();
    assertEquals(Result.SUCCESS_CODE, service.create(request).getCode());
    assertEquals("interview_scheduled", application.getStatus());
    verify(applications).updateById(application);
    ArgumentCaptor<InterviewInterviewer> assigned = ArgumentCaptor.forClass(InterviewInterviewer.class);
    verify(interviewers).insert(assigned.capture());
    assertEquals(21, assigned.getValue().getInterviewId());
    assertEquals(101, assigned.getValue().getInterviewerId());
  }
}
