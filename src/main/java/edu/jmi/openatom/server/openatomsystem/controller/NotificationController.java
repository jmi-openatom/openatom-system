package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  @GetMapping("/notifications")
  @SaCheckPermission("notification:list")
  public ApiResponse<List<Notification>> notifications() {
    return notificationService.currentUserNotifications();
  }

  @PostMapping("/notifications")
  @SaCheckPermission("notification:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateNotificationDTO request) {
    return notificationService.create(request);
  }

  @PostMapping("/notifications/{notificationId}/read")
  @SaCheckPermission("notification:read")
  public ApiResponse<String> markRead(@PathVariable Integer notificationId) {
    return notificationService.markRead(notificationId);
  }
}
