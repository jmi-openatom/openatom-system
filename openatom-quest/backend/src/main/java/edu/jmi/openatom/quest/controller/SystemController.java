package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
            "service", "openatom-quest",
            "status", "UP",
            "time", Instant.now().toString()
        ));
    }
}
