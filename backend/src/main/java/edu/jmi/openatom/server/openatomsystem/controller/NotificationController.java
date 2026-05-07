package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  @GetMapping
  public ApiResponse<List<ResponseNotificationDTO>> notifications() {
    return notificationService.currentUserNotifications();
  }

  @GetMapping("/unread-count")
  public ApiResponse<Integer> unreadCount() {
    return notificationService.unreadCount();
  }

  @PostMapping("/admin")
  @SaCheckPermission("notification:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateNotificationDTO request) {
    return notificationService.create(request);
  }

  @PostMapping("/{notificationId}/read")
  public ApiResponse<String> markRead(@PathVariable Integer notificationId) {
    return notificationService.markRead(notificationId);
  }

  @GetMapping("/admin")
  @SaCheckPermission("notification:list")
  public ApiResponse<List<Notification>> listAll() {
    return notificationService.listAll();
  }

  @DeleteMapping("/admin/{notificationId}")
  @SaCheckPermission("notification:delete")
  public ApiResponse<String> delete(@PathVariable Integer notificationId) {
    return notificationService.delete(notificationId);
  }
}
