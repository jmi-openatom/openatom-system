package edu.jmi.openatom.lab.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lab_checkin_score_log")
public class CheckinScoreLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate checkinDate;

    @Column(nullable = false)
    private Integer attendanceStatus = 0; // 0-未签到, 1-正常(刷题AC), 2-正常(现场), 3-迟到

    private Integer localScoreChange = 0;

    @Column(nullable = false)
    private Integer clubSyncStatus = 0; // 0-无需同步, 1-待同步, 2-同步成功, 3-同步失败

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
