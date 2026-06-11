package edu.jmi.openatom.lab.controller;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.lab.dto.ApiResponse;
import edu.jmi.openatom.lab.entity.Submission;
import edu.jmi.openatom.lab.service.JudgeService;
import edu.jmi.openatom.lab.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final JudgeService judgeService;
    private final SubmissionRepository submissionRepository;

    @PostMapping
    public ApiResponse<Submission> submit(@RequestBody Map<String, Object> request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long problemId = Long.valueOf(request.get("problemId").toString());
        String code = (String) request.get("code");
        String language = (String) request.get("language");

        Submission submission = judgeService.judge(userId, problemId, code, language);
        return ApiResponse.success(submission);
    }

    @GetMapping("/my")
    public ApiResponse<List<Submission>> getMySubmissions() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Submission> submissions = submissionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ApiResponse.success(submissions);
    }
}
