package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.OnboardingProfileRequest;
import edu.jmi.openatom.quest.dto.UpdateOnboardingRequest;
import edu.jmi.openatom.quest.entity.Member;
import edu.jmi.openatom.quest.entity.OnboardingProgress;
import edu.jmi.openatom.quest.mapper.AccessMapper;
import edu.jmi.openatom.quest.mapper.MemberMapper;
import edu.jmi.openatom.quest.mapper.OnboardingProgressMapper;
import edu.jmi.openatom.quest.mapper.TechnicalDirectionMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OnboardingFlowTest {
    private Member member;
    private OnboardingProgress progress;
    private OnboardingService service;

    @BeforeEach
    void setUp() {
        MemberMapper members = mock(MemberMapper.class);
        OnboardingProgressMapper progresses = mock(OnboardingProgressMapper.class);
        TechnicalDirectionMapper directions = mock(TechnicalDirectionMapper.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        member = Member.builder().id(1L).nickname("LMS 成员").school("LMS 学校").status("ACTIVE").build();
        progress = new OnboardingProgress();
        progress.setMemberId(1L);
        progress.setCurrentStep(1);
        progress.setCompletedStepsJson("[]");
        when(members.selectById(1L)).thenReturn(member);
        when(progresses.selectById(1L)).thenReturn(progress);
        when(directions.selectCount(any())).thenReturn(1L);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(1L);
        ObjectMapper mapper = new ObjectMapper();
        ProfileService profiles = new ProfileService(members, directions, mock(AccessMapper.class), mapper, jdbc);
        service = new OnboardingService(progresses, members, mapper, profiles);
    }

    @Test
    void newMemberCompletesProfileWithinSevenSteps() {
        OnboardingProfileRequest profile = profile(true, List.of(10L));
        assertThat(member.getProfileCompletedAt()).isNull();
        for (int step = 1; step <= 3; step++) {
            service.completeStep(1L, request(step, profile));
            assertThat(member.getProfileCompletedAt()).isNull();
        }
        service.completeStep(1L, request(4, profile));
        assertThat(member.getProfileCompletedAt()).isNotNull();
        assertThat(member.getNickname()).isEqualTo("新人昵称");
        assertThat(member.getSchool()).isEqualTo("LMS 学校");
        assertThat(member.getSkillsJson()).isEqualTo("[\"Git\"]");
        assertThat(member.getWeeklyHours()).isEqualTo(6);
        for (int step = 5; step <= 6; step++) service.completeStep(1L, request(step, profile));
        assertThat(member.getOnboardingCompletedAt()).isNull();
        service.completeStep(1L, request(7, profile));
        assertThat(member.getOnboardingCompletedAt()).isNotNull();
        assertThat(progress.getCompletedStepsJson()).isEqualTo("[1,2,3,4,5,6,7]");
    }

    @Test
    void cannotSkipAgreementOrDirectionAndFailedStepDoesNotAdvance() {
        assertThatThrownBy(() -> service.completeStep(1L, request(3, profile(true, List.of(10L)))))
            .hasMessageContaining("按顺序");
        service.completeStep(1L, request(1, profile(false, List.of())));
        assertThatThrownBy(() -> service.completeStep(1L, request(2, profile(false, List.of()))))
            .hasMessageContaining("行为准则");
        assertThat(progress.getCurrentStep()).isEqualTo(2);
        service.completeStep(1L, request(2, profile(true, List.of())));
        assertThatThrownBy(() -> service.completeStep(1L, request(3, profile(true, List.of()))))
            .hasMessageContaining("至少选择一个");
        assertThat(progress.getCurrentStep()).isEqualTo(3);
        assertThat(member.getOnboardingCompletedAt()).isNull();
    }

    private OnboardingProfileRequest profile(boolean agreed, List<Long> directions) {
        return new OnboardingProfileRequest("新人昵称", agreed, directions, List.of("Git"), "https://github.com/example", 6, "想参与开源协作");
    }

    private UpdateOnboardingRequest request(int step, OnboardingProfileRequest profile) {
        return new UpdateOnboardingRequest(step, "刚开始学习 Git", "beginner", profile);
    }
}
