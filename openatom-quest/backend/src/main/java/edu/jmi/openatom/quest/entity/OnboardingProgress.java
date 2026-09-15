package edu.jmi.openatom.quest.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("quest_onboarding_progress")
public class OnboardingProgress {
    @TableId
    private Long memberId;
    private Integer currentStep;
    private String completedStepsJson;
    private String selfAssessmentJson;
    private String assessmentResultJson;
    private String draftJson;
    private LocalDateTime updatedAt;
}
