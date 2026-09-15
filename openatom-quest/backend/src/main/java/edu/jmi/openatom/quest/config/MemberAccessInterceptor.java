package edu.jmi.openatom.quest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.MemberIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MemberAccessInterceptor implements HandlerInterceptor {
    public static final String CURRENT_MEMBER = "quest.current-member";
    private static final String MEMBER_ID = "member.id";

    private final MemberIdentityService memberIdentityService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute(MEMBER_ID);
        if (!(id instanceof Long memberId)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return false;
        }
        CurrentMember member = memberIdentityService.getCurrent(memberId);
        if (member == null || !"ACTIVE".equals(member.status())) {
            session.invalidate();
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "账号已被禁用");
            return false;
        }
        request.setAttribute(CURRENT_MEMBER, member);
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
