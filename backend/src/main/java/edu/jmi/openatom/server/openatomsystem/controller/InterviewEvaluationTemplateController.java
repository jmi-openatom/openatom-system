package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveInterviewEvaluationTemplateDTO;
import edu.jmi.openatom.server.openatomsystem.service.InterviewEvaluationTemplateService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewEvaluationTemplateVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interview-evaluation-templates")
@RequiredArgsConstructor
public class InterviewEvaluationTemplateController {
  private final InterviewEvaluationTemplateService service;

  @GetMapping
  @SaCheckPermission("interview:list")
  public Result<List<ResponseInterviewEvaluationTemplateVO>> list(@RequestParam Integer campaignId) {
    return service.list(campaignId);
  }

  @GetMapping("/active")
  @SaCheckPermission("interview:feedback")
  public Result<ResponseInterviewEvaluationTemplateVO> active(@RequestParam Integer campaignId) {
    return service.active(campaignId);
  }

  @PostMapping
  @SaCheckPermission("interview:update")
  public Result<ResponseInterviewEvaluationTemplateVO> save(
      @Valid @RequestBody RequestSaveInterviewEvaluationTemplateDTO request) {
    return service.save(request);
  }
}
