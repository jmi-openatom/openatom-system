package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import java.util.List;

public interface NotificationService {
  ApiResponse<List<ResponseNotificationDTO>> currentUserNotifications();

  ApiResponse<String> create(RequestCreateNotificationDTO request);

  ApiResponse<String> markRead(Integer notificationId);

  ApiResponse<List<Notification>> listAll();

  ApiResponse<String> delete(Integer notificationId);

  ApiResponse<Integer> unreadCount();
}
