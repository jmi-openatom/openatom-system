package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewQueueOperation;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRoom;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewSession;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class InterviewRoomLocationTest {
  private final InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
  private final InterviewRoomMapper rooms = mock(InterviewRoomMapper.class);
  private final InterviewMapper interviews = mock(InterviewMapper.class);
  private final InterviewQueueOperationMapper operations = mock(InterviewQueueOperationMapper.class);
  private final InterviewQueueServiceImpl service = new InterviewQueueServiceImpl(
      sessions, rooms, interviews, mock(InterviewQueueStateMapper.class), operations,
      mock(InterviewRoomInterviewerMapper.class), mock(InterviewInterviewerMapper.class),
      mock(InterviewFeedbackMapper.class), mock(MembershipApplicationMapper.class), mock(UserMapper.class));
  private StpLogic originalLogic;

  @BeforeEach
  void setUpAuthentication() {
    originalLogic = StpUtil.getStpLogic();
    StpLogic logic = mock(StpLogic.class);
    when(logic.getLoginType()).thenReturn("login");
    StpUtil.setStpLogic(logic);
  }

  @AfterEach
  void restoreAuthentication() {
    StpUtil.setStpLogic(originalLogic);
  }

  @Test
  void updatesRoomAndItsInterviewLocationsAndRecordsPreviousLocation() {
    InterviewRoom room = prepareRoom("published", null);
    String location = "韦立楼 " + "A".repeat(250);

    assertEquals(0, service.updateRoomLocation(7, "  " + location + "  ").getCode());

    assertEquals(location, room.getLocation());
    verify(rooms).updateById(room);
    verify(interviews).updateLocationByRoomId(3, 7, location);
    verifyNoMoreInteractions(interviews);
    ArgumentCaptor<InterviewQueueOperation> entry = ArgumentCaptor.forClass(InterviewQueueOperation.class);
    verify(operations).insert(entry.capture());
    assertEquals("update_room_location", entry.getValue().getAction());
    assertEquals(3, entry.getValue().getSessionId());
    assertEquals(7, entry.getValue().getRoomId());
    assertEquals("", Jsons.parseObject(entry.getValue().getDetailJson()).get("fromLocation"));
    assertEquals(location, Jsons.parseObject(entry.getValue().getDetailJson()).get("toLocation"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "\t\n", "　"})
  void rejectsBlankLocationWithoutWriting(String location) {
    assertEquals(400, service.updateRoomLocation(7, location).getCode());
    verifyNoInteractions(rooms, interviews, operations);
  }

  @Test
  void rejectsLocationLongerThanDatabaseColumn() {
    assertEquals(400, service.updateRoomLocation(7, "A".repeat(256)).getCode());
    verifyNoInteractions(rooms, interviews, operations);
  }

  @Test
  void rejectsMissingRoom() {
    assertEquals(404, service.updateRoomLocation(7, "A201").getCode());
    verifyNoInteractions(interviews, operations);
  }

  @ParameterizedTest
  @ValueSource(strings = {"draft", "completed"})
  void allowsDraftAndClosedSessionEdits(String status) {
    prepareRoom(status, "待定");

    assertEquals(0, service.updateRoomLocation(7, "A201").getCode());

    verify(interviews).updateLocationByRoomId(3, 7, "A201");
  }

  @Test
  void unchangedLocationDoesNotWriteOrAddAnOperation() {
    prepareRoom("published", "A201");

    assertEquals(0, service.updateRoomLocation(7, " A201 ").getCode());

    verify(rooms, never()).updateById(any(InterviewRoom.class));
    verifyNoInteractions(interviews, operations);
  }

  private InterviewRoom prepareRoom(String status, String location) {
    InterviewRoom room = InterviewRoom.builder().id(7).sessionId(3).location(location).build();
    when(rooms.selectByIdForUpdate(7)).thenReturn(room);
    when(sessions.selectById(3)).thenReturn(InterviewSession.builder().id(3).status(status).build());
    return room;
  }
}
