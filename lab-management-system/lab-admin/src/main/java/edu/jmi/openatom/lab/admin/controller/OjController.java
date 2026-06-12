package edu.jmi.openatom.lab.admin.controller;

import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.common.web.Result;
import edu.jmi.openatom.lab.oj.service.OjService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oj")
public class OjController {
  private final OjService ojService;

  @GetMapping("/today")
  public Result<LabDtos.ProblemView> today() {
    return Result.success(ojService.today());
  }

  @PostMapping("/submissions")
  public Result<LabDtos.SubmissionView> submit(@Valid @RequestBody LabDtos.SubmitCodeRequest request) {
    return Result.success(ojService.submit(request));
  }

  @GetMapping("/submissions")
  public Result<List<LabDtos.SubmissionView>> mySubmissions() {
    return Result.success(ojService.mySubmissions());
  }

  @GetMapping("/submissions/{id}")
  public Result<LabDtos.SubmissionView> submission(@PathVariable Long id) {
    return Result.success(ojService.submission(id));
  }
}
