package edu.jmi.openatom.lab.repository;

import edu.jmi.openatom.lab.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrUserIdIsNullOrderByCreatedAtDesc(Long userId);
}
