package edu.jmi.openatom.lab.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lab_users")
public class LabUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long clubUserId;

    private String username;
    private String avatar;
    private String email;

    @Column(nullable = false)
    private Integer labRole = 0; // 0-学生, 1-助教, 2-主教练

    @Column(nullable = false)
    private Integer reputationScore = 100;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
