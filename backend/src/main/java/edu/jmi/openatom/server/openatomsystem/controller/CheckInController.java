package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInRecordStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInScanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInTargetsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateCheckInSessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestEveningStudyScheduleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInGroupVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInSessionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseEveningStudyScheduleVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseEveningStudyTodayVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

  @GetMapping("/check-in-groups")
  @SaCheckPermission("check-in:create")
  public Result<List<ResponseCheckInGroupVO>> groups() {
    return checkInService.groups();
  }

  @PostMapping("/check-in-groups")
  @SaCheckPermission("check-in:create")
  public Result<Integer> createGroup(@Valid @RequestBody RequestCheckInGroupDTO request) {
    return checkInService.createGroup(request);
  }

  @PutMapping("/check-in-groups/{groupId}")
  @SaCheckPermission("check-in:create")
  public Result<String> updateGroup(
      @PathVariable Integer groupId, @Valid @RequestBody RequestCheckInGroupDTO request) {
    return checkInService.updateGroup(groupId, request);
  }

  @DeleteMapping("/check-in-groups/{groupId}")
  @SaCheckPermission("check-in:create")
  public Result<String> deleteGroup(@PathVariable Integer groupId) {
    return checkInService.deleteGroup(groupId);
  }

  @DeleteMapping("/check-in-groups/{groupId}/members/{userId}")
  @SaCheckPermission("check-in:group-member-delete")
  public Result<String> removeGroupMember(@PathVariable Integer groupId, @PathVariable Integer userId) {
    return checkInService.removeGroupMember(groupId, userId);
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

  @PatchMapping("/check-ins/{sessionId}")
  @SaCheckPermission("check-in:update")
  public Result<String> update(
      @PathVariable Integer sessionId, @Valid @RequestBody RequestCreateCheckInSessionDTO request) {
    return checkInService.update(sessionId, request);
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

  @PostMapping("/check-ins/{sessionId}/targets")
  @SaCheckPermission("check-in:update")
  public Result<String> addTargets(
      @PathVariable Integer sessionId, @Valid @RequestBody RequestCheckInTargetsDTO request) {
    return checkInService.addTargets(sessionId, request);
  }

  @GetMapping("/check-ins/{sessionId}/records")
  @SaCheckPermission("check-in:records")
  public Result<List<ResponseCheckInRecordVO>> records(@PathVariable Integer sessionId) {
    return checkInService.records(sessionId);
  }

  @PatchMapping("/check-ins/{sessionId}/records/{userId}")
  @SaCheckPermission("check-in:update")
  public Result<ResponseCheckInRecordVO> updateRecordStatus(
      @PathVariable Integer sessionId,
      @PathVariable Integer userId,
      @Valid @RequestBody RequestCheckInRecordStatusDTO request) {
    return checkInService.updateRecordStatus(sessionId, userId, request);
  }

  @PostMapping("/site/check-ins/scan")
  public Result<ResponseCheckInRecordVO> scan(@Valid @RequestBody RequestCheckInScanDTO request) {
    return checkInService.scan(request);
  }

  @GetMapping("/evening-study/schedules")
  @SaCheckPermission("check-in:list")
  public Result<List<ResponseEveningStudyScheduleVO>> eveningStudySchedules() {
    return checkInService.eveningStudySchedules();
  }

  @PostMapping("/evening-study/schedules")
  @SaCheckPermission("check-in:create")
  public Result<Integer> createEveningStudySchedule(
      @Valid @RequestBody RequestEveningStudyScheduleDTO request) {
    return checkInService.createEveningStudySchedule(request);
  }

  @PutMapping("/evening-study/schedules/{scheduleId}")
  @SaCheckPermission("check-in:update")
  public Result<String> updateEveningStudySchedule(
      @PathVariable Integer scheduleId,
      @Valid @RequestBody RequestEveningStudyScheduleDTO request) {
    return checkInService.updateEveningStudySchedule(scheduleId, request);
  }

  @DeleteMapping("/evening-study/schedules/{scheduleId}")
  @SaCheckPermission("check-in:delete")
  public Result<String> deleteEveningStudySchedule(@PathVariable Integer scheduleId) {
    return checkInService.deleteEveningStudySchedule(scheduleId);
  }

  @PostMapping("/evening-study/sessions/generate")
  @SaCheckPermission("check-in:create")
  public Result<ResponseEveningStudyTodayVO> generateEveningStudySessions(
      @RequestParam(required = false) String date) {
    return checkInService.generateEveningStudySessions(date);
  }

  @GetMapping("/evening-study/today")
  @SaCheckPermission("check-in:list")
  public Result<ResponseEveningStudyTodayVO> eveningStudyToday(
      @RequestParam(required = false) String date) {
    return checkInService.eveningStudyToday(date);
  }

  @GetMapping("/site/evening-study/today")
  public Result<ResponseEveningStudyTodayVO> siteEveningStudyToday(
      @RequestParam(required = false) String date) {
    return checkInService.eveningStudyToday(date);
  }

  @GetMapping("/bot/evening-study/today")
  public Result<ResponseEveningStudyTodayVO> botEveningStudyToday(
      @RequestParam(required = false) String date) {
    return checkInService.eveningStudyToday(date);
  }
}
