package edu.jmi.openatom.lab.admin.controller;

import edu.jmi.openatom.lab.checkin.service.CheckInService;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.common.web.Result;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CheckInController {
  private final CheckInService checkInService;

  @GetMapping("/admin/check-ins")
  public Result<List<LabDtos.CheckInSessionView>> list(@RequestParam(required = false) String status) {
    return Result.success(checkInService.list(status));
  }

  @GetMapping("/admin/check-ins/user-options")
  public Result<List<LabDtos.LabUserOption>> userOptions(@RequestParam(required = false) String keyword) {
    return Result.success(checkInService.userOptions(keyword));
  }

  @GetMapping("/admin/check-in-groups")
  public Result<List<LabDtos.CheckInGroupView>> groups() {
    return Result.success(checkInService.groups());
  }

  @PostMapping("/admin/check-in-groups")
  public Result<Long> createGroup(@Valid @RequestBody LabDtos.CheckInGroupRequest request) {
    return Result.success(checkInService.createGroup(request), "签到分组已创建");
  }

  @PutMapping("/admin/check-in-groups/{groupId}")
  public Result<String> updateGroup(
      @PathVariable Long groupId, @Valid @RequestBody LabDtos.CheckInGroupRequest request) {
    checkInService.updateGroup(groupId, request);
    return Result.success("签到分组已更新");
  }

  @DeleteMapping("/admin/check-in-groups/{groupId}")
  public Result<String> deleteGroup(@PathVariable Long groupId) {
    checkInService.deleteGroup(groupId);
    return Result.success("签到分组已删除");
  }

  @DeleteMapping("/admin/check-in-groups/{groupId}/members/{userId}")
  public Result<String> removeGroupMember(@PathVariable Long groupId, @PathVariable Long userId) {
    checkInService.removeGroupMember(groupId, userId);
    return Result.success("成员已移出分组");
  }

  @GetMapping("/admin/check-ins/{sessionId}")
  public Result<LabDtos.CheckInSessionView> detail(@PathVariable Long sessionId) {
    return Result.success(checkInService.detail(sessionId));
  }

  @PostMapping("/admin/check-ins")
  public Result<Long> create(@Valid @RequestBody LabDtos.CreateCheckInSessionRequest request) {
    return Result.success(checkInService.create(request), "签到已发布");
  }

  @PatchMapping("/admin/check-ins/{sessionId}")
  public Result<String> update(
      @PathVariable Long sessionId, @Valid @RequestBody LabDtos.CreateCheckInSessionRequest request) {
    checkInService.update(sessionId, request);
    return Result.success("签到已更新");
  }

  @PostMapping("/admin/check-ins/{sessionId}/close")
  public Result<String> close(@PathVariable Long sessionId) {
    checkInService.close(sessionId);
    return Result.success("签到已关闭");
  }

  @DeleteMapping("/admin/check-ins/{sessionId}")
  public Result<String> delete(@PathVariable Long sessionId) {
    checkInService.delete(sessionId);
    return Result.success("签到已删除");
  }

  @PostMapping("/admin/check-ins/{sessionId}/targets")
  public Result<String> addTargets(
      @PathVariable Long sessionId, @Valid @RequestBody LabDtos.CheckInTargetsRequest request) {
    checkInService.addTargets(sessionId, request);
    return Result.success("人员已添加");
  }

  @GetMapping("/admin/check-ins/{sessionId}/records")
  public Result<List<LabDtos.CheckInRecordView>> records(@PathVariable Long sessionId) {
    return Result.success(checkInService.records(sessionId));
  }

  @PatchMapping("/admin/check-ins/{sessionId}/records/{userId}")
  public Result<LabDtos.CheckInRecordView> updateRecordStatus(
      @PathVariable Long sessionId,
      @PathVariable Long userId,
      @Valid @RequestBody LabDtos.CheckInRecordStatusRequest request) {
    return Result.success(checkInService.updateRecordStatus(sessionId, userId, request));
  }

  @PostMapping("/check-ins/scan")
  public Result<LabDtos.CheckInRecordView> scan(@Valid @RequestBody LabDtos.CheckInScanRequest request) {
    return Result.success(checkInService.scan(request));
  }

  @GetMapping("/admin/evening-study/schedules")
  public Result<List<LabDtos.EveningStudyScheduleView>> eveningStudySchedules() {
    return Result.success(checkInService.eveningStudySchedules());
  }

  @PostMapping("/admin/evening-study/schedules")
  public Result<Long> createEveningStudySchedule(
      @Valid @RequestBody LabDtos.EveningStudyScheduleRequest request) {
    return Result.success(checkInService.createEveningStudySchedule(request), "晚自习计划已创建");
  }

  @PutMapping("/admin/evening-study/schedules/{scheduleId}")
  public Result<String> updateEveningStudySchedule(
      @PathVariable Long scheduleId, @Valid @RequestBody LabDtos.EveningStudyScheduleRequest request) {
    checkInService.updateEveningStudySchedule(scheduleId, request);
    return Result.success("晚自习计划已更新");
  }

  @DeleteMapping("/admin/evening-study/schedules/{scheduleId}")
  public Result<String> deleteEveningStudySchedule(@PathVariable Long scheduleId) {
    checkInService.deleteEveningStudySchedule(scheduleId);
    return Result.success("晚自习计划已删除");
  }

  @PostMapping("/admin/evening-study/sessions/generate")
  public Result<LabDtos.EveningStudyTodayView> generateEveningStudySessions(
      @RequestParam(required = false) String date) {
    return Result.success(checkInService.generateEveningStudySessions(date), "晚自习签到已生成");
  }

  @GetMapping("/evening-study/today")
  public Result<LabDtos.EveningStudyTodayView> eveningStudyToday(
      @RequestParam(required = false) String date) {
    return Result.success(checkInService.eveningStudyToday(date));
  }
}
