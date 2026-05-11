package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedback;
import edu.jmi.openatom.server.openatomsystem.service.InterviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试控制器
 *
 * <p>提供面试的安排, 确认, 反馈及完成等操作
 */
@RestController
@RequiredArgsConstructor
public class InterviewController {
  private final InterviewService interviewService;

  /**
   * 获取面试列表
   *
   * @param campaignId 招新活动ID
   * @param applicationId 申请表ID
   * @param interviewerId 面试官ID
   * @param status 面试状态
   * @return 面试列表
   */
  @GetMapping("/interviews")
  @SaCheckPermission("interview:list")
  public ApiResponse<List<Interview>> list(
      @RequestParam(required = false) Integer campaignId,
      @RequestParam(required = false) Integer applicationId,
      @RequestParam(required = false) Integer interviewerId,
      @RequestParam(required = false) String status) {
    return interviewService.list(campaignId, applicationId, interviewerId, status);
  }

  /**
   * 创建面试
   *
   * @param request 创建面试请求
   * @return 操作结果
   */
  @PostMapping("/interviews")
  @SaCheckPermission("interview:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateInterviewDTO request) {
    return interviewService.create(request);
  }

  /**
   * 获取面试详情
   *
   * @param interviewId 面试ID
   * @return 面试详情
   */
  @GetMapping("/interviews/{interviewId}")
  @SaCheckPermission("interview:detail")
  public ApiResponse<Interview> detail(@PathVariable Integer interviewId) {
    return interviewService.detail(interviewId);
  }

  /**
   * 更新面试信息
   *
   * @param interviewId 面试ID
   * @param request 更新面试请求
   * @return 操作结果
   */
  @PatchMapping("/interviews/{interviewId}")
  @SaCheckPermission("interview:update")
  public ApiResponse<String> update(
      @PathVariable Integer interviewId, @Valid @RequestBody RequestUpdateInterviewDTO request) {
    return interviewService.update(interviewId, request);
  }

  /**
   * 确认面试
   *
   * @param interviewId 面试ID
   * @return 操作结果
   */
  @PostMapping("/interviews/{interviewId}/confirm")
  @SaCheckPermission("interview:confirm")
  public ApiResponse<String> confirm(@PathVariable Integer interviewId) {
    return interviewService.confirm(interviewId);
  }

  /**
   * 提交面试反馈
   *
   * @param interviewId 面试ID
   * @param request 面试反馈请求
   * @return 操作结果
   */
  @PostMapping("/interviews/{interviewId}/feedback")
  @SaCheckPermission("interview:feedback")
  public ApiResponse<String> feedback(
      @PathVariable Integer interviewId, @Valid @RequestBody RequestInterviewFeedbackDTO request) {
    return interviewService.feedback(interviewId, request);
  }

  /**
   * 完成面试
   *
   * @param interviewId 面试ID
   * @return 操作结果
   */
  @PostMapping("/interviews/{interviewId}/complete")
  @SaCheckPermission("interview:complete")
  public ApiResponse<String> complete(@PathVariable Integer interviewId) {
    return interviewService.complete(interviewId);
  }

  /**
   * 获取面试反馈列表
   *
   * @param interviewId 面试ID
   * @return 面试反馈列表
   */
  @GetMapping("/interviews/{interviewId}/feedbacks")
  @SaCheckPermission("interview:detail")
  public ApiResponse<List<InterviewFeedback>> feedbacks(@PathVariable Integer interviewId) {
    return interviewService.getFeedbacks(interviewId);
  }
}
