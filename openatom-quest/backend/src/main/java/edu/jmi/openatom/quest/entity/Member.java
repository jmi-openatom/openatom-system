package edu.jmi.openatom.quest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("quest_member")
public class Member {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String school;
    private String college;
    private String major;
    private String grade;
    private String skillsJson;
    private String codeProfileUrl;
    private Integer weeklyHours;
    private String bio;
    private LocalDateTime conductAgreedAt;
    private LocalDateTime profileCompletedAt;
    private LocalDateTime onboardingCompletedAt;
    private String status;
    private String currentLevel;
    private Integer totalPoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
