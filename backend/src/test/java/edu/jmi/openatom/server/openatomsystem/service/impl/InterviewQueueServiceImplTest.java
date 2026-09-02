package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewQueueState;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRoom;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewSession;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRoomInterviewer;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterviewQueueServiceImplTest {
  @Test
  void callNextIsBlockedUntilCurrentInterviewIsCompleted() {
    InterviewRoomMapper roomMapper = mock(InterviewRoomMapper.class);
    InterviewMapper interviewMapper = mock(InterviewMapper.class);
    InterviewQueueStateMapper queueMapper = mock(InterviewQueueStateMapper.class);
    InterviewSessionMapper sessionMapper = mock(InterviewSessionMapper.class);
    MembershipApplicationMapper applicationMapper = mock(MembershipApplicationMapper.class);
    InterviewRoom room = InterviewRoom.builder().id(7).sessionId(3).name("第一面试间").build();
    InterviewQueueState current = InterviewQueueState.builder().id(11L).interviewId(21)
        .roomId(7).sessionId(3).status("called").build();
    when(roomMapper.selectByIdForUpdate(7)).thenReturn(room);
    when(sessionMapper.selectById(3)).thenReturn(InterviewSession.builder().id(3).status("published").build());
    when(queueMapper.selectActiveByRoomId(7)).thenReturn(List.of(current));
    when(interviewMapper.selectById(21)).thenReturn(Interview.builder().id(21).status("confirmed").build());
    InterviewQueueServiceImpl service = new InterviewQueueServiceImpl(
        sessionMapper, roomMapper, interviewMapper, queueMapper,
        mock(InterviewQueueOperationMapper.class), mock(InterviewRoomInterviewerMapper.class),
        mock(InterviewInterviewerMapper.class), mock(InterviewFeedbackMapper.class),
        applicationMapper, mock(UserMapper.class));

    var result = service.callNext(7);

    assertEquals(422, result.getCode());
    assertEquals("上一位候选人的所有面试官尚未提交评价，暂不能叫下一位", result.getMessage());
    verifyNoInteractions(applicationMapper);
  }

  @Test
  void moveRoomRebindsCandidateToTargetRoomInterviewerGroup() {
    InterviewSessionMapper sessionMapper = mock(InterviewSessionMapper.class);
    InterviewRoomMapper roomMapper = mock(InterviewRoomMapper.class);
    InterviewMapper interviewMapper = mock(InterviewMapper.class);
    InterviewQueueStateMapper queueMapper = mock(InterviewQueueStateMapper.class);
    InterviewQueueOperationMapper operationMapper = mock(InterviewQueueOperationMapper.class);
    InterviewRoomInterviewerMapper roomInterviewerMapper = mock(InterviewRoomInterviewerMapper.class);
    InterviewInterviewerMapper interviewerMapper = mock(InterviewInterviewerMapper.class);
    Interview interview = Interview.builder().id(30).sessionId(3).roomId(7).location("旧房间")
        .status("confirmed").build();
    InterviewQueueState state = InterviewQueueState.builder().id(12L).interviewId(30)
        .sessionId(3).roomId(7).status("waiting").build();
    when(interviewMapper.selectById(30)).thenReturn(interview);
    when(sessionMapper.selectById(3)).thenReturn(InterviewSession.builder().id(3).status("published").build());
    when(roomMapper.selectByIdForUpdate(8)).thenReturn(
        InterviewRoom.builder().id(8).sessionId(3).name("第二面试间").location("B202").build());
    when(queueMapper.selectByInterviewId(30)).thenReturn(state);
    when(roomInterviewerMapper.selectByRoomId(8)).thenReturn(List.of(
        InterviewRoomInterviewer.builder().roomId(8).interviewerId(101).build(),
        InterviewRoomInterviewer.builder().roomId(8).interviewerId(102).build()));
    InterviewQueueServiceImpl service = new InterviewQueueServiceImpl(sessionMapper, roomMapper,
        interviewMapper, queueMapper, operationMapper, roomInterviewerMapper, interviewerMapper,
        mock(InterviewFeedbackMapper.class), mock(MembershipApplicationMapper.class), mock(UserMapper.class));

    var result = service.moveRoom(30, 8);

    assertEquals(0, result.getCode());
    assertEquals(8, interview.getRoomId());
    assertEquals(8, state.getRoomId());
    verify(interviewerMapper).deleteByInterviewId(30);
    verify(interviewerMapper, org.mockito.Mockito.times(2)).insert(any());
  }
}
