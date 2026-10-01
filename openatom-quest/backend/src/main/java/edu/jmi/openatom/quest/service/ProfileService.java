package edu.jmi.openatom.quest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.UpdateProfileRequest;
import edu.jmi.openatom.quest.dto.OnboardingProfileRequest;
import edu.jmi.openatom.quest.entity.Member;
import edu.jmi.openatom.quest.entity.TechnicalDirection;
import edu.jmi.openatom.quest.mapper.AccessMapper;
import edu.jmi.openatom.quest.mapper.MemberMapper;
import edu.jmi.openatom.quest.mapper.TechnicalDirectionMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final MemberMapper memberMapper;
    private final TechnicalDirectionMapper directionMapper;
    private final AccessMapper accessMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getProfile(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("成员不存在");
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("nickname", member.getNickname());
        profile.put("avatarUrl", member.getAvatarUrl());
        profile.put("school", member.getSchool());
        profile.put("college", member.getCollege());
        profile.put("major", member.getMajor());
        profile.put("grade", member.getGrade());
        profile.put("skills", readStringList(member.getSkillsJson()));
        profile.put("codeProfileUrl", member.getCodeProfileUrl());
        profile.put("weeklyHours", member.getWeeklyHours());
        profile.put("bio", member.getBio());
        profile.put("conductAgreed", member.getConductAgreedAt() != null);
        profile.put("directionIds", jdbcTemplate.queryForList(
            "SELECT direction_id FROM quest_member_direction WHERE member_id = ? ORDER BY is_primary DESC, created_at",
            Long.class,
            memberId
        ));
        return profile;
    }

    @Transactional
    public void updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = memberMapper.selectById(memberId);
        if (member == null || !"ACTIVE".equals(member.getStatus())) {
            throw new IllegalStateException("成员不存在或已被禁用");
        }
        List<Long> directionIds = new LinkedHashSet<>(request.directionIds()).stream().toList();
        long activeDirectionCount = directionMapper.selectCount(
            new LambdaQueryWrapper<TechnicalDirection>()
                .in(TechnicalDirection::getId, directionIds)
                .eq(TechnicalDirection::getStatus, "ACTIVE")
        );
        if (activeDirectionCount != directionIds.size()) {
            throw new IllegalArgumentException("包含不存在或已归档的技术方向");
        }

        member.setNickname(request.nickname().trim());
        member.setAvatarUrl(trimToNull(request.avatarUrl()));
        member.setSkillsJson(writeJson(request.skills() == null ? List.of() : request.skills()));
        member.setCodeProfileUrl(trimToNull(request.codeProfileUrl()));
        member.setWeeklyHours(request.weeklyHours());
        member.setBio(trimToNull(request.bio()));
        if (member.getConductAgreedAt() == null) {
            member.setConductAgreedAt(LocalDateTime.now());
        }
        member.setProfileCompletedAt(LocalDateTime.now());
        memberMapper.updateById(member);

        accessMapper.deleteMemberDirections(memberId);
        for (int index = 0; index < directionIds.size(); index++) {
            accessMapper.addMemberDirection(memberId, directionIds.get(index), index == 0);
        }
    }

    @Transactional
    public void updateOnboardingProfile(Long memberId, int step, OnboardingProfileRequest request) {
        Member member = memberMapper.selectById(memberId);
        if (member == null || !"ACTIVE".equals(member.getStatus())) {
            throw new IllegalStateException("成员不存在或已被禁用");
        }
        // 已完成资料的旧版客户端仍可继续未完成的引导。
        if (request == null) {
            if (member.getProfileCompletedAt() == null) throw new IllegalArgumentException("请在引导中完善资料");
            return;
        }
        switch (step) {
            case 1 -> {
                if (request.nickname() == null || request.nickname().isBlank()) {
                    throw new IllegalArgumentException("请填写姓名或社团昵称");
                }
                member.setNickname(request.nickname().trim());
            }
            case 2 -> {
                if (!Boolean.TRUE.equals(request.conductAgreed())) {
                    throw new IllegalArgumentException("请先同意成员行为准则");
                }
                if (member.getConductAgreedAt() == null) member.setConductAgreedAt(LocalDateTime.now());
            }
            case 3 -> {
                if (request.directionIds() == null || request.directionIds().isEmpty()) {
                    throw new IllegalArgumentException("至少选择一个技术方向");
                }
                List<Long> directionIds = new LinkedHashSet<>(request.directionIds()).stream().toList();
                long activeCount = directionMapper.selectCount(new LambdaQueryWrapper<TechnicalDirection>()
                    .in(TechnicalDirection::getId, directionIds).eq(TechnicalDirection::getStatus, "ACTIVE"));
                if (activeCount != directionIds.size()) throw new IllegalArgumentException("包含不存在或已归档的技术方向");
                accessMapper.deleteMemberDirections(memberId);
                for (int index = 0; index < directionIds.size(); index++) {
                    accessMapper.addMemberDirection(memberId, directionIds.get(index), index == 0);
                }
            }
            case 4 -> {
                requireOnboardingBasics(member);
                member.setSkillsJson(writeJson(request.skills() == null ? List.of() : request.skills()));
                member.setCodeProfileUrl(trimToNull(request.codeProfileUrl()));
                member.setWeeklyHours(request.weeklyHours());
                member.setBio(trimToNull(request.bio()));
                member.setProfileCompletedAt(LocalDateTime.now());
            }
            default -> { return; }
        }
        memberMapper.updateById(member);
    }

    public void requireOnboardingProfileComplete(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null || member.getProfileCompletedAt() == null) {
            throw new IllegalArgumentException("请先完成引导中的资料填写");
        }
        requireOnboardingBasics(member);
    }

    private void requireOnboardingBasics(Member member) {
        if (member.getNickname() == null || member.getNickname().isBlank() || member.getConductAgreedAt() == null) {
            throw new IllegalArgumentException("请先确认基本资料和成员行为准则");
        }
        Long directionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM quest_member_direction WHERE member_id = ?", Long.class, member.getId());
        if (directionCount == null || directionCount == 0) throw new IllegalArgumentException("至少选择一个技术方向");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("资料格式无效", exception);
        }
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("成员技能数据损坏", exception);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
