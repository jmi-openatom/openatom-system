package edu.jmi.openatom.lab.controller;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.lab.dto.ApiResponse;
import edu.jmi.openatom.lab.entity.Notification;
import edu.jmi.openatom.lab.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;

    @GetMapping
    public ApiResponse<List<Notification>> getMyNotifications() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Notification> notifications = notificationRepository
            .findByUserIdOrUserIdIsNullOrderByCreatedAtDesc(userId);
        return ApiResponse.success(notifications);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("通知不存在"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return ApiResponse.success(null);
    }
}
