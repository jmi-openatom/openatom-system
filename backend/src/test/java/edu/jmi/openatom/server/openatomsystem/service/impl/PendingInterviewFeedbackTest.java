package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.dto.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PendingInterviewFeedbackTest {
  @Test void lastSubmissionCompletesPendingFeedbackWithoutChangingNextCandidate() {
    InterviewMapper interviews = mock(InterviewMapper.class);
    InterviewInterviewerMapper assigned = mock(InterviewInterviewerMapper.class);
    InterviewFeedbackMapper feedback = mock(InterviewFeedbackMapper.class);
    InterviewQueueStateMapper queues = mock(InterviewQueueStateMapper.class);
    InterviewRoomMapper rooms = mock(InterviewRoomMapper.class);
    MembershipApplicationMapper applications = mock(MembershipApplicationMapper.class);
    Interview interview = Interview.builder().id(21).roomId(7).applicationId(31).status("confirmed").build();
    InterviewQueueState state = InterviewQueueState.builder().id(11L).interviewId(21).status("pending_feedback").build();
    MembershipApplication application = MembershipApplication.builder().id(31).status("interview_scheduled").build();
    InterviewFeedback own = InterviewFeedback.builder().id(1).interviewId(21).interviewerId(101).status("draft").build();
    InterviewFeedback other = InterviewFeedback.builder().id(2).interviewId(21).interviewerId(102).status("draft").build();
    when(interviews.selectById(21)).thenReturn(interview);
    when(rooms.selectByIdForUpdate(7)).thenReturn(InterviewRoom.builder().id(7).build());
    when(queues.selectByInterviewId(21)).thenReturn(state);
    when(applications.selectById(31)).thenReturn(application);
    when(assigned.selectByInterviewId(21)).thenReturn(List.of(
        InterviewInterviewer.builder().interviewId(21).interviewerId(101).build(),
        InterviewInterviewer.builder().interviewId(21).interviewerId(102).build()));
    when(feedback.selectLatest(21, 101)).thenReturn(own);
    when(feedback.selectLatest(21, 102)).thenReturn(other);
    var service = new InterviewServiceImpl(interviews, assigned, feedback,
        mock(InterviewFeedbackRevisionMapper.class), mock(InterviewEvaluationTemplateMapper.class),
        queues, rooms, applications, null, null);
    StpLogic original = StpUtil.getStpLogic();
    StpLogic auth = mock(StpLogic.class);
    when(auth.getLoginType()).thenReturn("login");
    when(auth.getLoginIdAsInt()).thenReturn(101);
    StpUtil.setStpLogic(auth);
    try {
      assertEquals(0, service.feedback(21, new RequestInterviewFeedbackDTO()).getCode());
      assertEquals("pending_feedback", state.getStatus());
      verify(queues, never()).updateById(any(InterviewQueueState.class));
      other.setStatus("submitted");
      assertEquals(0, service.feedback(21, new RequestInterviewFeedbackDTO()).getCode());
      assertEquals("completed", state.getStatus());
      assertEquals("completed", interview.getStatus());
      assertEquals("interviewed", application.getStatus());
      verify(queues).updateById(state);
      verify(rooms, times(2)).selectByIdForUpdate(7);
    } finally {
      StpUtil.setStpLogic(original);
    }
  }
}
