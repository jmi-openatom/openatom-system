package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import java.util.List;

public interface NotificationService {
  ApiResponse<List<Notification>> currentUserNotifications();

  ApiResponse<String> create(RequestCreateNotificationDTO request);

  ApiResponse<String> markRead(Integer notificationId);
}
