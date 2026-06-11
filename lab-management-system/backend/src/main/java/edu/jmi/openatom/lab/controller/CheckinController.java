package edu.jmi.openatom.lab.controller;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.lab.dto.ApiResponse;
import edu.jmi.openatom.lab.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {
    private final CheckinService checkinService;

    @PostMapping
    public ApiResponse<Void> checkin() {
        Long userId = StpUtil.getLoginIdAsLong();
        checkinService.manualCheckin(userId);
        return ApiResponse.success(null);
    }
}
