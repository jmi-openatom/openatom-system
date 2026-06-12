package edu.jmi.openatom.lab.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.notice.entity.LabNotification;
import edu.jmi.openatom.lab.notice.mapper.LabNotificationMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeService {
  private final LabNotificationMapper notificationMapper;

  public void sendToUser(Long userId, String type, String title, String content) {
    LabNotification notification = new LabNotification();
    notification.setUserId(userId);
    notification.setNoticeType(type);
    notification.setTitle(title);
    notification.setContent(content);
    notification.setCreatedAt(LocalDateTime.now());
    notificationMapper.insert(notification);
  }

  public void broadcastTemplate(String type, String title, String content) {
    LabNotification notification = new LabNotification();
    notification.setNoticeType(type);
    notification.setTitle(title);
    notification.setContent(content);
    notification.setCreatedAt(LocalDateTime.now());
    notificationMapper.insert(notification);
  }

  public List<LabDtos.NotificationView> listForUser(Long userId) {
    return notificationMapper
        .selectList(
            new LambdaQueryWrapper<LabNotification>()
                .and(w -> w.eq(LabNotification::getUserId, userId).or().isNull(LabNotification::getUserId))
                .orderByDesc(LabNotification::getCreatedAt)
                .last("limit 80"))
        .stream()
        .map(this::toView)
        .toList();
  }

  public void markRead(Long id, Long userId) {
    LabNotification notification = notificationMapper.selectById(id);
    if (notification == null) {
      return;
    }
    if (notification.getUserId() != null && !notification.getUserId().equals(userId)) {
      throw new IllegalArgumentException("无权读取该通知");
    }
    notification.setReadAt(LocalDateTime.now());
    notificationMapper.updateById(notification);
  }

  private LabDtos.NotificationView toView(LabNotification notification) {
    return new LabDtos.NotificationView(
        notification.getId(),
        notification.getUserId(),
        notification.getNoticeType(),
        notification.getTitle(),
        notification.getContent(),
        notification.getReadAt(),
        notification.getCreatedAt());
  }
}
