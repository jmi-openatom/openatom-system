package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.config.QuestProperties;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.MemberIdentityService;
import edu.jmi.openatom.quest.service.OpenAtomOauthClient;
import edu.jmi.openatom.quest.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String STATE = "oauth.state";
    private static final String NONCE = "oauth.nonce";
    private static final String VERIFIER = "oauth.verifier";
    private static final String MEMBER_ID = "member.id";
    private static final String ACCESS_TOKEN = "oauth.access-token";
    private static final String REFRESH_TOKEN = "oauth.refresh-token";

    private final OpenAtomOauthClient oauthClient;
    private final MemberIdentityService memberIdentityService;
    private final QuestProperties questProperties;
    private final AuditService auditService;

    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpSession session) {
        OpenAtomOauthClient.LoginRequest login = oauthClient.createLoginRequest();
        session.setAttribute(STATE, login.state());
        session.setAttribute(NONCE, login.nonce());
        session.setAttribute(VERIFIER, login.verifier());
        auditService.record(null, "OAUTH_LOGIN_START", "OAUTH_SESSION", session.getId(), Map.of("provider", "openatom"));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(login.authorizeUrl())).build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        HttpSession session
    ) {
        String expectedState = take(session, STATE);
        String nonce = take(session, NONCE);
        String verifier = take(session, VERIFIER);
        if (error != null || code == null || state == null || !state.equals(expectedState)
            || nonce == null || verifier == null) {
            auditService.record(null, "OAUTH_CALLBACK_REJECTED", "OAUTH_SESSION", session.getId(), Map.of("reason", error == null ? "invalid_callback" : error));
            return loginError(error == null ? "invalid_callback" : error);
        }
        try {
            OpenAtomOauthClient.TokenResult token = oauthClient.exchangeCode(code, verifier, nonce);
            CurrentMember member = memberIdentityService.findOrCreate(token.user());
            if (!"ACTIVE".equals(member.status())) {
                auditService.record(member.id(), "OAUTH_LOGIN_BLOCKED", "MEMBER", member.id(), Map.of("reason", "member_disabled"));
                session.invalidate();
                return loginError("member_disabled");
            }
            session.setAttribute(MEMBER_ID, member.id());
            session.setAttribute(ACCESS_TOKEN, token.accessToken());
            session.setAttribute(REFRESH_TOKEN, token.refreshToken());
            auditService.record(member.id(), "OAUTH_LOGIN_SUCCESS", "MEMBER", member.id(), Map.of("provider", "openatom"));
            String next = member.profileCompleted() ? (member.onboardingCompleted() ? "/dashboard" : "/onboarding") : "/profile/setup";
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl() + next))
                .build();
        } catch (RuntimeException exception) {
            auditService.record(null, "OAUTH_LOGIN_FAILED", "OAUTH_SESSION", session.getId(), Map.of("errorType", exception.getClass().getSimpleName()));
            session.invalidate();
            return loginError("oauth_exchange_failed");
        }
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<CurrentMember>> session(HttpSession session) {
        Object value = session.getAttribute(MEMBER_ID);
        if (!(value instanceof Long memberId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(false, null, "未登录"));
        }
        CurrentMember member = memberIdentityService.getCurrent(memberId);
        if (member == null || !"ACTIVE".equals(member.status())) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(false, null, "账号已被禁用"));
        }
        return ResponseEntity.ok(ApiResponse.ok(member));
    }

    @GetMapping("/identity")
    public ApiResponse<Map<String, String>> identity(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(memberIdentityService.getOwnOauthIdentity(member.id()));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object memberId = session.getAttribute(MEMBER_ID);
            auditService.record(memberId instanceof Long id ? id : null, "OAUTH_LOGOUT", "OAUTH_SESSION", session.getId(), Map.of());
            session.invalidate();
        }
        return ApiResponse.ok(Map.of("redirect", frontendUrl() + "/login"));
    }

    private String take(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        session.removeAttribute(key);
        return value instanceof String text ? text : null;
    }

    private ResponseEntity<Void> loginError(String error) {
        String encoded = URLEncoder.encode(error, StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(frontendUrl() + "/login?error=" + encoded))
            .build();
    }

    private String frontendUrl() {
        return questProperties.frontendUrl().replaceAll("/+$", "");
    }
}
