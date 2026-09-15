package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.config.OauthProperties;
import edu.jmi.openatom.quest.entity.Member;
import edu.jmi.openatom.quest.entity.OauthIdentity;
import edu.jmi.openatom.quest.mapper.AccessMapper;
import edu.jmi.openatom.quest.mapper.MemberMapper;
import edu.jmi.openatom.quest.mapper.OauthIdentityMapper;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.model.OauthUserInfo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class MemberIdentityServiceTest {

    @Test
    void lmsAdminReceivesQuestAdminRoleAndEducationProfileIsSynchronized() {
        MemberMapper memberMapper = mock(MemberMapper.class);
        OauthIdentityMapper identityMapper = mock(OauthIdentityMapper.class);
        AccessMapper accessMapper = mock(AccessMapper.class);
        OauthProperties oauthProperties = new OauthProperties(
            "https://oauth.example.test", "quest", null, "https://quest.example.test/callback",
            "openid profile", null
        );
        List<String> auditActions = new ArrayList<>();
        AuditService auditService = new AuditService(new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                auditActions.add(String.valueOf(args[1]));
                return 1;
            }
        }, new ObjectMapper());
        MemberIdentityService service = new MemberIdentityService(
            memberMapper, identityMapper, accessMapper, oauthProperties, auditService
        );
        OauthIdentity identity = OauthIdentity.builder()
            .memberId(7L)
            .provider("openatom")
            .subject("42")
            .firstLoginAt(LocalDateTime.now())
            .build();
        Member member = Member.builder().id(7L).nickname("成员").status("ACTIVE").build();
        when(identityMapper.findByProviderAndSubject("openatom", "42")).thenReturn(identity);
        when(memberMapper.selectById(7L)).thenReturn(member);
        when(accessMapper.findRoleId("ADMIN")).thenReturn(4L);
        when(accessMapper.assignRole(7L, 4L)).thenReturn(1);
        when(accessMapper.findRoleKeys(7L)).thenReturn(List.of("MEMBER", "ADMIN"));
        when(accessMapper.findPermissionKeys(7L)).thenReturn(List.of("task:read", "stats:global"));

        CurrentMember current = service.findOrCreate(new OauthUserInfo(
            "42", "成员", null, "member@example.test", "江苏海事职业技术学院",
            "信息工程学院", "软件技术", "2026级", 2
        ));

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateById(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getCollege()).isEqualTo("信息工程学院");
        assertThat(memberCaptor.getValue().getMajor()).isEqualTo("软件技术");
        assertThat(memberCaptor.getValue().getGrade()).isEqualTo("2026级");
        verify(accessMapper).assignRole(7L, 4L);
        assertThat(auditActions).containsExactly("LMS_ADMIN_ASSIGNED");
        assertThat(current.roles()).contains("ADMIN");
    }
}
