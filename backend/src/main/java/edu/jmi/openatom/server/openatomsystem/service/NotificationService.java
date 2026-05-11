package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import java.util.List;

/**
 * 通知消息服务接口
 *
 * <p>定义当前用户通知列表, 创建通知, 标记已读, 全部通知查询, 删除通知以及未读数量统计等业务操作
 */
public interface NotificationService {
  ApiResponse<List<ResponseNotificationDTO>> currentUserNotifications();

  ApiResponse<String> create(RequestCreateNotificationDTO request);

  ApiResponse<String> markRead(Integer notificationId);

  ApiResponse<List<Notification>> listAll();

  ApiResponse<String> delete(Integer notificationId);

  ApiResponse<Integer> unreadCount();
}
