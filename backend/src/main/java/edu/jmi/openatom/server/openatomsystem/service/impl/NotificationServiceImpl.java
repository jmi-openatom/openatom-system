package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseNotificationVO;
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

/**
 * 通知管理实现类
 *
 * <p>负责通知的发送（单人/全员）, 已读标记, 未读统计, 通知列表查询以及通知删除等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private final NotificationMapper notificationMapper;
  private final NotificationReceiverMapper notificationReceiverMapper;
  private final UserMapper userMapper;

  @Override
  public Result<List<ResponseNotificationVO>> currentUserNotifications() {
    Integer userId = StpUtil.getLoginIdAsInt();
    List<NotificationReceiver> receivers = notificationReceiverMapper.selectByReceiverUserId(userId);
    if (receivers.isEmpty()) return Result.success(List.of());
    List<Integer> notificationIds = receivers.stream().map(NotificationReceiver::getNotificationId).toList();
    List<Notification> notifications = notificationMapper.selectBatchIds(notificationIds);
    Map<Integer, Notification> notificationMap = notifications.stream().collect(Collectors.toMap(Notification::getId, n -> n));
    List<ResponseNotificationVO> result = new ArrayList<>();
    for (NotificationReceiver receiver : receivers) {
      Notification n = notificationMap.get(receiver.getNotificationId());
      if (n != null) {
        result.add(ResponseNotificationVO.builder().id(n.getId()).title(n.getTitle()).content(n.getContent())
            .type(n.getType()).createdAt(n.getCreatedAt()).readFlag(receiver.getReadFlag()).readAt(receiver.getReadAt()).build());
      }
    }
    result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    return Result.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> create(RequestCreateNotificationDTO request) {
    if (Boolean.TRUE.equals(request.getIsAll())) { /* System-wide notification */ }
    else {
      if (request.getReceiverUserIds() == null || request.getReceiverUserIds().isEmpty()) return Result.error(400, "receiverUserIds不能为空");
      for (Integer receiverUserId : request.getReceiverUserIds()) { if (userMapper.selectById(receiverUserId) == null) return Result.error(400, "存在无效的接收用户"); }
    }
    Notification notification = Notification.builder().title(request.getTitle()).content(request.getContent()).type(request.getType()).build();
    int row = notificationMapper.insert(notification);
    if (row <= 0) return Result.error("通知发送失败");
    if (Boolean.TRUE.equals(request.getIsAll())) {
      List<User> allUsers = userMapper.selectList(null);
      for (User user : allUsers) { notificationReceiverMapper.insert(NotificationReceiver.builder().notificationId(notification.getId()).receiverUserId(user.getId()).readFlag(0).build()); }
    } else {
      for (Integer receiverUserId : request.getReceiverUserIds().stream().distinct().toList()) {
        notificationReceiverMapper.insert(NotificationReceiver.builder().notificationId(notification.getId()).receiverUserId(receiverUserId).readFlag(0).build());
      }
    }
    return Result.success("通知发送成功");
  }

  @Override
  public Result<String> markRead(Integer notificationId) {
    Integer userId = StpUtil.getLoginIdAsInt();
    NotificationReceiver receiver = notificationReceiverMapper.selectOneByNotificationAndUser(notificationId, userId);
    if (receiver == null) return Result.error(404, "通知不存在");
    receiver.setReadFlag(1);
    receiver.setReadAt(Times.now());
    notificationReceiverMapper.updateByNotificationAndUser(receiver, notificationId, userId);
    return Result.success("通知已读");
  }

  @Override
  public Result<List<Notification>> listAll() {
    return Result.success(notificationMapper.selectAllOrdered());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> delete(Integer notificationId) {
    notificationMapper.deleteById(notificationId);
    notificationReceiverMapper.deleteByNotificationId(notificationId);
    return Result.success("删除成功");
  }

  @Override
  public Result<Integer> unreadCount() {
    Integer userId = StpUtil.getLoginIdAsInt();
    Long count = notificationReceiverMapper.countUnread(userId);
    return Result.success(count.intValue());
  }
}
