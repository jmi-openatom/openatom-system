package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.StpLogic;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ForceCallNextTest {
  final InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
  final InterviewRoomMapper rooms = mock(InterviewRoomMapper.class);
  final InterviewMapper interviews = mock(InterviewMapper.class);
  final InterviewQueueStateMapper queues = mock(InterviewQueueStateMapper.class);
  final InterviewQueueOperationMapper operations = mock(InterviewQueueOperationMapper.class);
  final InterviewFeedbackMapper feedback = mock(InterviewFeedbackMapper.class);
  final MembershipApplicationMapper applications = mock(MembershipApplicationMapper.class);
  final InterviewQueueServiceImpl service = new InterviewQueueServiceImpl(sessions, rooms,
      interviews, queues, operations, mock(InterviewRoomInterviewerMapper.class),
      mock(InterviewInterviewerMapper.class), feedback, applications, mock(UserMapper.class));
  final Interview previous = Interview.builder().id(21).sessionId(3).roomId(7)
      .status("confirmed").queueNumber(1).build();
  final Interview successor = Interview.builder().id(22).sessionId(3).roomId(7)
      .status("confirmed").queueNumber(2).build();
  final InterviewQueueState current = InterviewQueueState.builder().id(11L).interviewId(21)
      .sessionId(3).roomId(7).status("called").build();
  final InterviewQueueState next = InterviewQueueState.builder().id(12L).interviewId(22)
      .sessionId(3).roomId(7).status("waiting").build();

  ForceCallNextTest() {
    when(rooms.selectByIdForUpdate(7)).thenReturn(InterviewRoom.builder().id(7).sessionId(3).name("第一面试间").build());
    when(sessions.selectById(3)).thenReturn(InterviewSession.builder().id(3).status("published").build());
    when(queues.selectActiveByRoomId(7)).thenReturn(List.of(current));
    when(interviews.selectById(21)).thenReturn(previous);
    when(interviews.selectBySessionId(3)).thenReturn(List.of(previous, successor));
    when(queues.selectBySessionId(3)).thenReturn(List.of(current, next));
  }

  @Test void forcePreservesEvaluationAndLogsReason() {
    StpLogic original = StpUtil.getStpLogic();
    StpLogic auth = mock(StpLogic.class);
    when(auth.getLoginType()).thenReturn("login");
    when(auth.isLogin()).thenReturn(true);
    when(auth.getLoginIdAsInt()).thenReturn(101);
    StpUtil.setStpLogic(auth);
    try {
      var result = service.forceCallNext(7, 21, "  面试官临时离场  ");
      assertEquals(0, result.getCode());
      assertEquals(22, result.getData().getInterviewId());
      assertEquals("pending_feedback", current.getStatus());
      assertEquals("confirmed", previous.getStatus());
      assertEquals("called", next.getStatus());
      assertEquals(1, next.getCallCount());
      verify(interviews, never()).updateById(any(Interview.class));
      verifyNoInteractions(feedback);
      var log = ArgumentCaptor.forClass(InterviewQueueOperation.class);
      verify(operations).insert(log.capture());
      assertEquals("force_call_next", log.getValue().getAction());
      assertEquals(101, log.getValue().getOperatorId());
      var detail = Jsons.parseObject(log.getValue().getDetailJson());
      assertEquals("面试官临时离场", detail.get("reason"));
      assertEquals(21, detail.get("previousInterviewId"));
      when(queues.selectActiveByRoomId(7)).thenReturn(List.of(next));
      assertEquals(409, service.forceCallNext(7, 21, "重复操作").getCode());
      verify(queues, times(2)).updateById(any(InterviewQueueState.class));
    } finally {
      StpUtil.setStpLogic(original);
    }
  }

  @Test void noSuccessorLeavesCurrentUntouched() {
    next.setStatus("not_checked_in");
    assertEquals(422, service.forceCallNext(7, 21, "临时处理").getCode());
    assertEquals("called", current.getStatus());
    verify(queues, never()).updateById(any(InterviewQueueState.class));
    verifyNoInteractions(operations, feedback);
  }

  @Test void normalCallWithNoSuccessorAlsoPreservesCurrent() {
    previous.setStatus("completed");
    next.setStatus("not_checked_in");
    assertEquals(422, service.callNext(7).getCode());
    assertEquals("called", current.getStatus());
    verify(queues, never()).updateById(any(InterviewQueueState.class));
  }

  @Test void draftCandidatesCannotBeCalled() {
    successor.setStatus("draft");
    assertEquals(422, service.forceCallNext(7, 21, "临时处理").getCode());
    verify(queues, never()).updateById(any(InterviewQueueState.class));
  }

  @Test void staleRequestCannotSkipAnotherCandidate() {
    assertEquals(409, service.forceCallNext(7, 20, "页面已过期").getCode());
    verify(queues, never()).updateById(any(InterviewQueueState.class));
    verifyNoInteractions(operations);
  }

  @Test void reasonAndExpectedCandidateAreRequired() {
    assertEquals(400, service.forceCallNext(7, 21, "  ").getCode());
    assertEquals(400, service.forceCallNext(7, 21, "字".repeat(501)).getCode());
    assertEquals(400, service.forceCallNext(7, null, "临时处理").getCode());
    verifyNoInteractions(rooms, operations);
  }

  @Test void closedSessionCannotBeForced() {
    when(sessions.selectById(3)).thenReturn(InterviewSession.builder().id(3).status("completed").build());
    assertEquals(422, service.forceCallNext(7, 21, "临时处理").getCode());
    verify(queues, never()).updateById(any(InterviewQueueState.class));
  }

  @Test void pendingFeedbackPreventsClosingSession() {
    current.setStatus("pending_feedback");
    when(interviews.selectBySessionId(3)).thenReturn(List.of(previous));
    assertEquals(422, service.completeSession(3).getCode());
    verify(sessions, never()).updateById(any(InterviewSession.class));
  }
}
