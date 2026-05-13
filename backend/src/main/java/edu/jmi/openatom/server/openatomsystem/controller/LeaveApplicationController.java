package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.service.LeaveApplicationService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLeaveApplicationVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LeaveApplicationController {
  private final LeaveApplicationService leaveApplicationService;

  @GetMapping("/leave-applications")
  @SaCheckPermission("leave-application:list")
  public Result<List<ResponseLeaveApplicationVO>> list(@RequestParam(required = false) String status) {
    return leaveApplicationService.list(status);
  }

  @GetMapping("/site/leave-applications")
  public Result<List<ResponseLeaveApplicationVO>> mine() {
    return leaveApplicationService.mine();
  }

  @GetMapping("/leave-applications/{leaveApplicationId}")
  @SaCheckPermission("leave-application:detail")
  public Result<ResponseLeaveApplicationVO> detail(@PathVariable Integer leaveApplicationId) {
    return leaveApplicationService.detail(leaveApplicationId);
  }

  @GetMapping("/site/leave-applications/{leaveApplicationId}")
  public Result<ResponseLeaveApplicationVO> siteDetail(@PathVariable Integer leaveApplicationId) {
    return leaveApplicationService.detail(leaveApplicationId);
  }

  @PostMapping("/site/leave-applications")
  public Result<Integer> create(@Valid @RequestBody RequestCreateLeaveApplicationDTO request) {
    return leaveApplicationService.create(request);
  }

  @PostMapping("/leave-applications/{leaveApplicationId}/review")
  @SaCheckPermission("leave-application:review")
  public Result<String> review(
      @PathVariable Integer leaveApplicationId,
      @Valid @RequestBody RequestReviewLeaveApplicationDTO request) {
    return leaveApplicationService.review(leaveApplicationId, request);
  }
}
