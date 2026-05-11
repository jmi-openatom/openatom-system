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

/**
 * 社团活动控制器
 *
 * <p>提供社团活动的增删改查, 活动报名及报名列表查询等接口
 */
@RestController
@RequiredArgsConstructor
public class ActivityController {
  private final ActivityService activityService;

  /**
   * 获取活动列表, 支持按状态筛选
   *
   * @param status 活动状态
   * @return 活动列表
   */
  @GetMapping("/activities")
  @SaCheckPermission("activity:list")
  public ApiResponse<List<ClubActivity>> list(@RequestParam(required = false) String status) {
    return activityService.list(status);
  }

  /**
   * 获取活动详情
   *
   * @param activityId 活动ID
   * @return 活动详情
   */
  @GetMapping("/activities/{activityId}")
  @SaCheckPermission("activity:detail")
  public ApiResponse<ClubActivity> detail(@PathVariable Integer activityId) {
    return activityService.detail(activityId);
  }

  /**
   * 创建活动
   *
   * @param request 创建活动请求
   * @return 操作结果
   */
  @PostMapping("/activities")
  @SaCheckPermission("activity:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateActivityDTO request) {
    return activityService.create(request);
  }

  /**
   * 更新活动
   *
   * @param activityId 活动ID
   * @param request 更新活动请求
   * @return 操作结果
   */
  @PatchMapping("/activities/{activityId}")
  @SaCheckPermission("activity:update")
  public ApiResponse<String> update(
      @PathVariable Integer activityId, @Valid @RequestBody RequestUpdateActivityDTO request) {
    return activityService.update(activityId, request);
  }

  /**
   * 删除活动
   *
   * @param activityId 活动ID
   * @return 操作结果
   */
  @DeleteMapping("/activities/{activityId}")
  @SaCheckPermission("activity:delete")
  public ApiResponse<String> delete(@PathVariable Integer activityId) {
    return activityService.delete(activityId);
  }

  /**
   * 报名活动
   *
   * @param activityId 活动ID
   * @param request 报名信息
   * @return 操作结果
   */
  @PostMapping("/activities/{activityId}/registrations")
  public ApiResponse<String> register(
      @PathVariable Integer activityId,
      @RequestBody(required = false) RequestActivityRegistrationDTO request) {
    return activityService.register(activityId, request);
  }

  /**
   * 获取活动报名列表
   *
   * @param activityId 活动ID
   * @return 报名列表
   */
  @GetMapping("/activities/{activityId}/registrations")
  @SaCheckPermission("activity-registration:list")
  public ApiResponse<List<ActivityRegistration>> registrations(@PathVariable Integer activityId) {
    return activityService.registrations(activityId);
  }
}
