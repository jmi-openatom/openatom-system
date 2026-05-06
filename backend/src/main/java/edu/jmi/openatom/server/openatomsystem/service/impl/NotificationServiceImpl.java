package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import edu.jmi.openatom.server.openatomsystem.entity.NotificationReceiver;
import edu.jmi.openatom.server.openatomsystem.mapper.NotificationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.NotificationReceiverMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private final NotificationMapper notificationMapper;
  private final NotificationReceiverMapper notificationReceiverMapper;
  private final UserMapper userMapper;

  @Override
  public ApiResponse<List<Notification>> currentUserNotifications() {
    Integer userId = StpUtil.getLoginIdAsInt();
    List<Integer> notificationIds =
        notificationReceiverMapper
            .selectList(
                new LambdaQueryWrapper<NotificationReceiver>()
                    .eq(NotificationReceiver::getReceiverUserId, userId))
            .stream()
            .map(NotificationReceiver::getNotificationId)
            .toList();
    if (notificationIds.isEmpty()) {
      return ApiResponse.success(List.of());
    }
    return ApiResponse.success(notificationMapper.selectBatchIds(notificationIds));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> create(RequestCreateNotificationDTO request) {
    for (Integer receiverUserId : request.getReceiverUserIds()) {
      if (userMapper.selectById(receiverUserId) == null) {
        return ApiResponse.error(400, "存在无效的接收用户");
      }
    }
    Notification notification =
        Notification.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .type(request.getType())
            .build();
    int row = notificationMapper.insert(notification);
    if (row <= 0) {
      return ApiResponse.error("通知发送失败");
    }
    for (Integer receiverUserId : request.getReceiverUserIds().stream().distinct().toList()) {
      notificationReceiverMapper.insert(
          NotificationReceiver.builder()
              .notificationId(notification.getId())
              .receiverUserId(receiverUserId)
              .readFlag(0)
              .build());
    }
    return ApiResponse.success("通知发送成功");
  }

  @Override
  public ApiResponse<String> markRead(Integer notificationId) {
    Integer userId = StpUtil.getLoginIdAsInt();
    NotificationReceiver receiver =
        notificationReceiverMapper.selectOne(
            new LambdaQueryWrapper<NotificationReceiver>()
                .eq(NotificationReceiver::getNotificationId, notificationId)
                .eq(NotificationReceiver::getReceiverUserId, userId));
    if (receiver == null) {
      return ApiResponse.error(404, "通知不存在");
    }
    receiver.setReadFlag(1);
    receiver.setReadAt(Times.now());
    notificationReceiverMapper.update(
        receiver,
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getNotificationId, notificationId)
            .eq(NotificationReceiver::getReceiverUserId, userId));
    return ApiResponse.success("通知已读");
  }
}
