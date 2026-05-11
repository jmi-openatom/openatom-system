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

/**
 * 通知管理控制器
 *
 * <p>提供当前用户通知查询, 未读计数, 管理员创建通知, 标记已读及通知删除等功能
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  /**
   * 获取当前用户的通知列表
   *
   * @return 通知列表
   */
  @GetMapping
  public ApiResponse<List<ResponseNotificationDTO>> notifications() {
    return notificationService.currentUserNotifications();
  }

  /**
   * 获取当前用户未读通知数量
   *
   * @return 未读通知数量
   */
  @GetMapping("/unread-count")
  public ApiResponse<Integer> unreadCount() {
    return notificationService.unreadCount();
  }

  /**
   * 管理员创建通知
   *
   * @param request 创建通知请求参数
   * @return 创建结果
   */
  @PostMapping("/admin")
  @SaCheckPermission("notification:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateNotificationDTO request) {
    return notificationService.create(request);
  }

  /**
   * 将通知标记为已读
   *
   * @param notificationId 通知ID
   * @return 操作结果
   */
  @PostMapping("/{notificationId}/read")
  public ApiResponse<String> markRead(@PathVariable Integer notificationId) {
    return notificationService.markRead(notificationId);
  }

  /**
   * 管理员获取全部通知列表
   *
   * @return 全部通知列表
   */
  @GetMapping("/admin")
  @SaCheckPermission("notification:list")
  public ApiResponse<List<Notification>> listAll() {
    return notificationService.listAll();
  }

  /**
   * 管理员删除通知
   *
   * @param notificationId 通知ID
   * @return 删除结果
   */
  @DeleteMapping("/admin/{notificationId}")
  @SaCheckPermission("notification:delete")
  public ApiResponse<String> delete(@PathVariable Integer notificationId) {
    return notificationService.delete(notificationId);
  }
}
