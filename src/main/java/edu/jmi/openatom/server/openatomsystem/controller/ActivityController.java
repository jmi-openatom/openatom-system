package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestActivityRegistrationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ActivityRegistration;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.service.ActivityService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActivityController {
  private final ActivityService activityService;

  @GetMapping("/activities")
  @SaCheckPermission("activity:list")
  public ApiResponse<List<ClubActivity>> list(@RequestParam(required = false) String status) {
    return activityService.list(status);
  }

  @GetMapping("/activities/{activityId}")
  @SaCheckPermission("activity:detail")
  public ApiResponse<ClubActivity> detail(@PathVariable Integer activityId) {
    return activityService.detail(activityId);
  }

  @PostMapping("/activities")
  @SaCheckPermission("activity:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateActivityDTO request) {
    return activityService.create(request);
  }

  @PatchMapping("/activities/{activityId}")
  @SaCheckPermission("activity:update")
  public ApiResponse<String> update(
      @PathVariable Integer activityId, @Valid @RequestBody RequestUpdateActivityDTO request) {
    return activityService.update(activityId, request);
  }

  @DeleteMapping("/activities/{activityId}")
  @SaCheckPermission("activity:delete")
  public ApiResponse<String> delete(@PathVariable Integer activityId) {
    return activityService.delete(activityId);
  }

  @PostMapping("/activities/{activityId}/registrations")
  public ApiResponse<String> register(
      @PathVariable Integer activityId, @RequestBody(required = false) RequestActivityRegistrationDTO request) {
    return activityService.register(activityId, request);
  }

  @GetMapping("/activities/{activityId}/registrations")
  @SaCheckPermission("activity-registration:list")
  public ApiResponse<List<ActivityRegistration>> registrations(@PathVariable Integer activityId) {
    return activityService.registrations(activityId);
  }
}
