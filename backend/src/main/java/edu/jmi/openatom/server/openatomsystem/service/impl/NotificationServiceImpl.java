package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import edu.jmi.openatom.server.openatomsystem.entity.NotificationReceiver;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.NotificationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.NotificationReceiverMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
  public ApiResponse<List<ResponseNotificationDTO>> currentUserNotifications() {
    Integer userId = StpUtil.getLoginIdAsInt();
    List<NotificationReceiver> receivers =
        notificationReceiverMapper.selectList(
            new LambdaQueryWrapper<NotificationReceiver>()
                .eq(NotificationReceiver::getReceiverUserId, userId));
    if (receivers.isEmpty()) {
      return ApiResponse.success(List.of());
    }
    List<Integer> notificationIds =
        receivers.stream().map(NotificationReceiver::getNotificationId).toList();
    List<Notification> notifications = notificationMapper.selectBatchIds(notificationIds);
    Map<Integer, Notification> notificationMap =
        notifications.stream().collect(Collectors.toMap(Notification::getId, n -> n));

    List<ResponseNotificationDTO> result = new ArrayList<>();
    for (NotificationReceiver receiver : receivers) {
      Notification n = notificationMap.get(receiver.getNotificationId());
      if (n != null) {
        result.add(
            ResponseNotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType())
                .createdAt(n.getCreatedAt())
                .readFlag(receiver.getReadFlag())
                .readAt(receiver.getReadAt())
                .build());
      }
    }
    result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    return ApiResponse.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> create(RequestCreateNotificationDTO request) {
    if (Boolean.TRUE.equals(request.getIsAll())) {
      // System-wide notification
    } else {
      if (request.getReceiverUserIds() == null || request.getReceiverUserIds().isEmpty()) {
        return ApiResponse.error(400, "receiverUserIds不能为空");
      }
      for (Integer receiverUserId : request.getReceiverUserIds()) {
        if (userMapper.selectById(receiverUserId) == null) {
          return ApiResponse.error(400, "存在无效的接收用户");
        }
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
    if (Boolean.TRUE.equals(request.getIsAll())) {
      List<User> allUsers = userMapper.selectList(null);
      for (User user : allUsers) {
        notificationReceiverMapper.insert(
            NotificationReceiver.builder()
                .notificationId(notification.getId())
                .receiverUserId(user.getId())
                .readFlag(0)
                .build());
      }
    } else {
      for (Integer receiverUserId : request.getReceiverUserIds().stream().distinct().toList()) {
        notificationReceiverMapper.insert(
            NotificationReceiver.builder()
                .notificationId(notification.getId())
                .receiverUserId(receiverUserId)
                .readFlag(0)
                .build());
      }
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

  @Override
  public ApiResponse<List<Notification>> listAll() {
    return ApiResponse.success(
        notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>().orderByDesc(Notification::getCreatedAt)));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> delete(Integer notificationId) {
    notificationMapper.deleteById(notificationId);
    notificationReceiverMapper.delete(
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getNotificationId, notificationId));
    return ApiResponse.success("删除成功");
  }

  @Override
  public ApiResponse<Integer> unreadCount() {
    Integer userId = StpUtil.getLoginIdAsInt();
    Long count = notificationReceiverMapper.selectCount(
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getReceiverUserId, userId)
            .eq(NotificationReceiver::getReadFlag, 0));
    return ApiResponse.success(count.intValue());
  }
}
