package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseClubHomeDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseRecruitmentDetailDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseRecruitmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteFormDetailDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteProgressDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.service.SiteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台站点控制器
 *
 * <p>提供社团主页, 活动列表, 招新信息, 表单详情及用户进度等公开接口
 */
@RestController
@RequiredArgsConstructor
public class SiteController {
  private final SiteService siteService;

  /**
   * 获取社团首页信息
   *
   * @param clubCode 社团编码（可选）
   * @return 社团首页信息
   */
  @GetMapping("/site/club-home")
  public ApiResponse<ResponseClubHomeDTO> getClubHome(
      @RequestParam(required = false) String clubCode) {
    return siteService.getClubHome(clubCode);
  }

  /**
   * 获取社团活动列表
   *
   * @return 活动列表
   */
  @GetMapping("/site/activities")
  public ApiResponse<List<ClubActivity>> getActivities() {
    return siteService.getActivities();
  }

  /**
   * 获取活动详情
   *
   * @param activityId 活动ID
   * @return 活动详情
   */
  @GetMapping("/site/activities/{activityId}")
  public ApiResponse<ClubActivity> getActivityDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer activityId) {
    return siteService.getActivityDetail(activityId);
  }

  /**
   * 获取招新信息列表
   *
   * @param clubId 社团ID（可选）
   * @return 招新信息
   */
  @GetMapping("/site/recruitment")
  public ApiResponse<ResponseRecruitmentDTO> getRecruitment(
      @RequestParam(required = false) Integer clubId) {
    return siteService.getRecruitment(clubId);
  }

  /**
   * 获取招新活动详情
   *
   * @param campaignId 招新活动ID
   * @return 招新活动详情
   */
  @GetMapping("/site/recruitment/{campaignId}")
  public ApiResponse<ResponseRecruitmentDetailDTO> getRecruitmentDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer campaignId) {
    return siteService.getRecruitmentDetail(campaignId);
  }

  /**
   * 获取报名表单详情
   *
   * @param campaignId 招新活动ID
   * @return 表单详情
   */
  @GetMapping("/site/forms/{campaignId}")
  public ApiResponse<ResponseSiteFormDetailDTO> getFormDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer campaignId) {
    return siteService.getFormDetail(campaignId);
  }

  /**
   * 获取当前用户的报名进度
   *
   * @return 用户进度信息
   */
  @GetMapping("/site/progress")
  public ApiResponse<ResponseSiteProgressDTO> getMyProgress() {
    return siteService.getMyProgress();
  }

  /**
   * 查询注册功能是否开启
   *
   * @return 注册是否开启
   */
  @GetMapping("/site/register-enabled")
  public ApiResponse<Boolean> getRegisterEnabled() {
    return siteService.getRegisterEnabled();
  }
}
