package edu.jmi.openatom.lab.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.common.web.Result;
import edu.jmi.openatom.lab.framework.properties.LabAiProperties;
import edu.jmi.openatom.lab.framework.properties.LabMqProperties;
import edu.jmi.openatom.lab.framework.properties.LabSandboxProperties;
import edu.jmi.openatom.lab.oj.entity.LabSubmission;
import edu.jmi.openatom.lab.oj.mapper.LabSubmissionMapper;
import edu.jmi.openatom.lab.oj.service.OjService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
  private final OjService ojService;
  private final LabAiProperties aiProperties;
  private final LabSandboxProperties sandboxProperties;
  private final LabMqProperties mqProperties;
  private final LabSubmissionMapper submissionMapper;

  @GetMapping("/problems")
  public Result<List<LabDtos.ProblemView>> problems() {
    return Result.success(ojService.recentProblems());
  }

  @PostMapping("/problems")
  public Result<LabDtos.ProblemView> saveProblem(@Valid @RequestBody LabDtos.AdminProblemRequest request) {
    return Result.success(ojService.saveProblem(request), "题目已保存");
  }

  @PostMapping("/problems/generate-today")
  public Result<LabDtos.ProblemView> generateToday() {
    return Result.success(ojService.generateToday(), "今日题目已生成");
  }

  @GetMapping("/monitor/status")
  public Result<LabDtos.MonitorStatus> monitorStatus() {
    Integer pending =
        Math.toIntExact(
            submissionMapper.selectCount(
                new LambdaQueryWrapper<LabSubmission>().eq(LabSubmission::getJudgeStatus, "PENDING")));
    return Result.success(
        new LabDtos.MonitorStatus(
            aiProperties.isEnabled(),
            sandboxProperties.getEndpoint() != null && !sandboxProperties.getEndpoint().isBlank(),
            mqProperties.getExchange(),
            mqProperties.getClubScoreRoutingKey(),
            pending));
  }
}
