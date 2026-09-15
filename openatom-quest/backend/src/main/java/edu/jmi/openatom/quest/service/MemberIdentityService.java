package edu.jmi.openatom.quest.service;

import edu.jmi.openatom.quest.entity.Member;
import edu.jmi.openatom.quest.entity.OauthIdentity;
import edu.jmi.openatom.quest.config.OauthProperties;
import edu.jmi.openatom.quest.mapper.AccessMapper;
import edu.jmi.openatom.quest.mapper.MemberMapper;
import edu.jmi.openatom.quest.mapper.OauthIdentityMapper;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.model.OauthUserInfo;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberIdentityService {
    private static final String PROVIDER = "openatom";

    private final MemberMapper memberMapper;
    private final OauthIdentityMapper identityMapper;
    private final AccessMapper accessMapper;
    private final OauthProperties oauthProperties;
    private final AuditService auditService;

    @Transactional
    public synchronized CurrentMember findOrCreate(OauthUserInfo userInfo) {
        OauthIdentity identity = identityMapper.findByProviderAndSubject(PROVIDER, userInfo.subject());
        LocalDateTime now = LocalDateTime.now();
        Member member;
        if (identity == null) {
            member = Member.builder()
                .nickname(userInfo.displayName())
                .avatarUrl(userInfo.avatarUrl())
                .email(userInfo.email())
                .status("ACTIVE")
                .currentLevel("L0")
                .totalPoints(0)
                .build();
            memberMapper.insert(member);

            identity = OauthIdentity.builder()
                .memberId(member.getId())
                .provider(PROVIDER)
                .subject(userInfo.subject())
                .displayName(userInfo.displayName())
                .avatarUrl(userInfo.avatarUrl())
                .email(userInfo.email())
                .firstLoginAt(now)
                .lastLoginAt(now)
                .build();
            identityMapper.insert(identity);

            Long memberRoleId = accessMapper.findRoleId("MEMBER");
            accessMapper.assignRole(member.getId(), memberRoleId);
            accessMapper.createOnboarding(member.getId());
        } else {
            identity.setDisplayName(userInfo.displayName());
            identity.setAvatarUrl(userInfo.avatarUrl());
            identity.setEmail(userInfo.email());
            identity.setLastLoginAt(now);
            identityMapper.updateById(identity);
            member = memberMapper.selectById(identity.getMemberId());
        }
        if (oauthProperties.isBootstrapAdminSubject(userInfo.subject())) {
            Long adminRoleId = accessMapper.findRoleId("ADMIN");
            if (accessMapper.assignRole(member.getId(), adminRoleId) == 1) {
                auditService.record(
                    member.getId(),
                    "BOOTSTRAP_ADMIN_ASSIGNED",
                    "MEMBER",
                    member.getId(),
                    Map.of("provider", PROVIDER)
                );
            }
        }
        return toCurrentMember(member);
    }

    public CurrentMember getCurrent(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return null;
        }
        return toCurrentMember(member);
    }

    public Map<String, String> getOwnOauthIdentity(Long memberId) {
        OauthIdentity identity = identityMapper.findOpenAtomByMemberId(memberId);
        if (identity == null) {
            throw new IllegalStateException("OpenAtom OAuth 身份关联不存在");
        }
        return Map.of("provider", identity.getProvider(), "subject", identity.getSubject());
    }

    private CurrentMember toCurrentMember(Member member) {
        return new CurrentMember(
            member.getId(),
            member.getNickname(),
            member.getAvatarUrl(),
            member.getStatus(),
            member.getCurrentLevel(),
            member.getTotalPoints(),
            member.getProfileCompletedAt() != null,
            member.getOnboardingCompletedAt() != null,
            accessMapper.findRoleKeys(member.getId()),
            accessMapper.findPermissionKeys(member.getId())
        );
    }
}
