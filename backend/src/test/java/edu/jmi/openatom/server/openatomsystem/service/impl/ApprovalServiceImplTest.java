package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestApprovalActionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBatchApprovalDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.mapper.ApprovalRecordMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class ApprovalServiceImplTest {
  private final ApprovalRecordMapper records = mock(ApprovalRecordMapper.class);
  private final MembershipApplicationMapper applications = mock(MembershipApplicationMapper.class);
  private final NotificationService notifications = mock(NotificationService.class);
  private final MailBroadcastPlanner mail = mock(MailBroadcastPlanner.class);
  private final ApprovalServiceImpl service = new ApprovalServiceImpl(records, applications, notifications, mail);
  private StpLogic originalAuth;

  @BeforeEach
  void setUpAuth() {
    originalAuth = StpUtil.getStpLogic();
    StpLogic auth = mock(StpLogic.class);
    when(auth.getLoginType()).thenReturn("login");
    when(auth.getLoginIdAsInt()).thenReturn(101);
    StpUtil.setStpLogic(auth);
  }

  @AfterEach
  void restoreAuth() {
    StpUtil.setStpLogic(originalAuth);
  }

  @ParameterizedTest
  @CsvSource({"approve,pre_screen_passed", "reject,pre_screen_rejected",
      "transfer,submitted", "request_more_info,submitted"})
  void anonymousApplicationPersistsApprovalWithoutSendingNotifications(String action, String status) {
    MembershipApplication application = application(4, null);
    assertEquals(Result.SUCCESS_CODE, service.approve(4, request(action)).getCode());
    assertEquals(status, application.getStatus());
    verify(applications).updateById(application);
    ArgumentCaptor<ApprovalRecord> record = ArgumentCaptor.forClass(ApprovalRecord.class);
    verify(records).insert(record.capture());
    assertEquals(4, record.getValue().getApplicationId());
    assertEquals(101, record.getValue().getOperatorId());
    assertEquals("pre_screen", record.getValue().getNode());
    assertEquals(action, record.getValue().getAction());
    assertEquals("审核意见", record.getValue().getComment());
    verifyNoInteractions(notifications, mail);
  }

  @Test
  void boundApplicationStillNotifiesItsUser() {
    application(4, 201);
    assertEquals(Result.SUCCESS_CODE, service.approve(4, request("approve")).getCode());
    verifyUserNotification();
  }

  @Test
  void batchCanApproveAnonymousAndBoundApplicationsTogether() {
    MembershipApplication anonymous = application(4, null);
    MembershipApplication bound = application(5, 201);
    RequestBatchApprovalDTO batch = RequestBatchApprovalDTO.builder()
        .applicationIds(List.of(4, 5)).approval(request("approve")).build();
    assertEquals(Result.SUCCESS_CODE, service.batchApprove(batch).getCode());
    assertEquals("pre_screen_passed", anonymous.getStatus());
    assertEquals("pre_screen_passed", bound.getStatus());
    verify(applications).updateById(anonymous);
    verify(applications).updateById(bound);
    verify(records, times(2)).insert(any(ApprovalRecord.class));
    verifyUserNotification();
  }

  private MembershipApplication application(Integer id, Integer userId) {
    MembershipApplication application = MembershipApplication.builder()
        .id(id).userId(userId).status("submitted").build();
    when(applications.selectById(id)).thenReturn(application);
    return application;
  }

  private RequestApprovalActionDTO request(String action) {
    return RequestApprovalActionDTO.builder().action(action).node("pre_screen").comment("审核意见").build();
  }

  private void verifyUserNotification() {
    ArgumentCaptor<RequestCreateNotificationDTO> notification =
        ArgumentCaptor.forClass(RequestCreateNotificationDTO.class);
    verify(notifications).create(notification.capture());
    assertEquals(List.of(201), notification.getValue().getReceiverUserIds());
    assertEquals("approval", notification.getValue().getType());
    assertEquals("您的入会申请有了新的动态：当前状态为【pre_screen_passed】。\n审核意见：审核意见",
        notification.getValue().getContent());
    verify(mail).enqueueUserMail(any(String.class), eq(201), eq("approval"),
        eq("申请进度更新"), eq(notification.getValue().getContent()));
    verifyNoMoreInteractions(notifications, mail);
  }
}
