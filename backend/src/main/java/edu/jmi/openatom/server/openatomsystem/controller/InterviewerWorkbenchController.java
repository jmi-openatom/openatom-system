package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.InterviewerWorkbenchService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewerWorkbenchItemVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InterviewerWorkbenchController {
  private final InterviewerWorkbenchService service;

  @GetMapping("/interviewer-workbench")
  @SaCheckPermission("interview:feedback")
  public Result<List<ResponseInterviewerWorkbenchItemVO>> list() { return service.list(); }
}
