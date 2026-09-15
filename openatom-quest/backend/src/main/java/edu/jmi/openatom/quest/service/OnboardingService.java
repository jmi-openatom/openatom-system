package edu.jmi.openatom.quest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.UpdateOnboardingRequest;
import edu.jmi.openatom.quest.entity.Member;
import edu.jmi.openatom.quest.entity.OnboardingProgress;
import edu.jmi.openatom.quest.mapper.MemberMapper;
import edu.jmi.openatom.quest.mapper.OnboardingProgressMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final OnboardingProgressMapper progressMapper;
    private final MemberMapper memberMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public OnboardingProgress completeStep(Long memberId, UpdateOnboardingRequest request) {
        OnboardingProgress progress = progressMapper.selectById(memberId);
        if (progress == null) {
            throw new IllegalStateException("新人引导记录不存在");
        }
        Set<Integer> completed = readSteps(progress.getCompletedStepsJson());
        int firstIncomplete = firstIncomplete(completed);
        if (request.step() > firstIncomplete) {
            throw new IllegalArgumentException("请按顺序完成新人引导");
        }
        completed.add(request.step());
        progress.setCompletedStepsJson(writeJson(completed));
        progress.setCurrentStep(firstIncomplete(completed));
        progress.setSelfAssessmentJson(writeJson(Map.of(
            "skillNote", request.skillNote() == null ? "" : request.skillNote(),
            "assessment", request.assessment() == null ? "" : request.assessment()
        )));
        progressMapper.updateById(progress);

        if (completed.size() == 7) {
            Member member = memberMapper.selectById(memberId);
            if (member != null && member.getOnboardingCompletedAt() == null) {
                member.setOnboardingCompletedAt(LocalDateTime.now());
                memberMapper.updateById(member);
            }
        }
        return progress;
    }

    public OnboardingProgress get(Long memberId) {
        return progressMapper.selectById(memberId);
    }

    private Set<Integer> readSteps(String json) {
        try {
            return new LinkedHashSet<>(objectMapper.readValue(json, new TypeReference<Set<Integer>>() {}));
        } catch (Exception exception) {
            throw new IllegalStateException("新人引导进度损坏", exception);
        }
    }

    private int firstIncomplete(Set<Integer> completed) {
        for (int step = 1; step <= 7; step++) {
            if (!completed.contains(step)) return step;
        }
        return 7;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("无法保存新人引导进度", exception);
        }
    }
}
