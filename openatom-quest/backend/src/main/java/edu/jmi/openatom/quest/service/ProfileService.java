package edu.jmi.openatom.quest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.UpdateProfileRequest;
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
        member.setSchool(request.school().trim());
        member.setCollege(request.college().trim());
        member.setMajor(request.major().trim());
        member.setGrade(request.grade().trim());
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
