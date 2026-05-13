package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInScanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateCheckInSessionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInSessionVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CheckInController {
  private final CheckInService checkInService;

  @GetMapping("/check-ins")
  @SaCheckPermission("check-in:list")
  public Result<List<ResponseCheckInSessionVO>> list(@RequestParam(required = false) String status) {
    return checkInService.list(status);
  }

  @GetMapping("/check-ins/user-options")
  @SaCheckPermission("check-in:create")
  public Result<List<User>> userOptions(@RequestParam(required = false) String keyword) {
    return checkInService.userOptions(keyword);
  }

  @GetMapping("/check-ins/{sessionId}")
  @SaCheckPermission("check-in:detail")
  public Result<ResponseCheckInSessionVO> detail(@PathVariable Integer sessionId) {
    return checkInService.detail(sessionId);
  }

  @PostMapping("/check-ins")
  @SaCheckPermission("check-in:create")
  public Result<Integer> create(@Valid @RequestBody RequestCreateCheckInSessionDTO request) {
    return checkInService.create(request);
  }

  @PostMapping("/check-ins/{sessionId}/close")
  @SaCheckPermission("check-in:update")
  public Result<String> close(@PathVariable Integer sessionId) {
    return checkInService.close(sessionId);
  }

  @DeleteMapping("/check-ins/{sessionId}")
  @SaCheckPermission("check-in:delete")
  public Result<String> delete(@PathVariable Integer sessionId) {
    return checkInService.delete(sessionId);
  }

  @GetMapping("/check-ins/{sessionId}/records")
  @SaCheckPermission("check-in:records")
  public Result<List<ResponseCheckInRecordVO>> records(@PathVariable Integer sessionId) {
    return checkInService.records(sessionId);
  }

  @PostMapping("/site/check-ins/scan")
  public Result<ResponseCheckInRecordVO> scan(@Valid @RequestBody RequestCheckInScanDTO request) {
    return checkInService.scan(request);
  }
}
