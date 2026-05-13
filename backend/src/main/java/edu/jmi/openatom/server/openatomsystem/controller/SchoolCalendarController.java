package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveSchoolCalendarAdjustmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveSchoolCalendarDTO;
import edu.jmi.openatom.server.openatomsystem.service.SchoolCalendarService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSchoolCalendarVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SchoolCalendarController {
  private final SchoolCalendarService schoolCalendarService;

  @GetMapping("/school-calendar")
  @SaCheckPermission("school-calendar:manage")
  public Result<ResponseSchoolCalendarVO> detail() {
    return schoolCalendarService.detail();
  }

  @GetMapping("/site/school-calendar")
  public Result<ResponseSchoolCalendarVO> siteDetail() {
    return schoolCalendarService.detail();
  }

  @PostMapping("/school-calendar")
  @SaCheckPermission("school-calendar:manage")
  public Result<ResponseSchoolCalendarVO> save(@Valid @RequestBody RequestSaveSchoolCalendarDTO request) {
    return schoolCalendarService.save(request);
  }

  @PostMapping("/school-calendar/adjustments")
  @SaCheckPermission("school-calendar:manage")
  public Result<ResponseSchoolCalendarVO> saveAdjustment(
      @Valid @RequestBody RequestSaveSchoolCalendarAdjustmentDTO request) {
    return schoolCalendarService.saveAdjustment(request);
  }

  @DeleteMapping("/school-calendar/adjustments/{adjustmentId}")
  @SaCheckPermission("school-calendar:manage")
  public Result<String> deleteAdjustment(@PathVariable Integer adjustmentId) {
    return schoolCalendarService.deleteAdjustment(adjustmentId);
  }
}
