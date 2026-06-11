package edu.jmi.openatom.lab.controller;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.lab.dto.ApiResponse;
import edu.jmi.openatom.lab.entity.LabUser;
import edu.jmi.openatom.lab.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String cmsToken = request.get("cmsToken");
        LabUser user = authService.loginWithCMSToken(cmsToken);

        return ApiResponse.success(Map.of(
            "token", StpUtil.getTokenValue(),
            "user", user
        ));
    }

    @GetMapping("/me")
    public ApiResponse<LabUser> getCurrentUser() {
        LabUser user = authService.getCurrentUser();
        return ApiResponse.success(user);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.success(null);
    }
}
