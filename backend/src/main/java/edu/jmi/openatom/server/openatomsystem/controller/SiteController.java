package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.service.SiteService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseActivationVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubHomeVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseRecruitmentDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseRecruitmentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteFormDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteProgressVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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
  public Result<ResponseClubHomeVO> getClubHome(
      @RequestParam(required = false) String clubCode) {
    return siteService.getClubHome(clubCode);
  }

  /**
   * 获取社团活动列表
   *
   * @return 活动列表
   */
  @GetMapping("/site/activities")
  public Result<List<ClubActivity>> getActivities() {
    return siteService.getActivities();
  }

  /**
   * 获取活动详情
   *
   * @param activityId 活动ID
   * @return 活动详情
   */
  @GetMapping("/site/activities/{activityId}")
  public Result<ClubActivity> getActivityDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer activityId) {
    return siteService.getActivityDetail(activityId);
  }

  /** 获取公开展示的活跃社团列表 */
  @GetMapping("/site/clubs")
  public Result<List<Club>> getPublicClubs() {
    return siteService.getPublicClubs();
  }

  /**
   * 获取招新信息列表
   *
   * @param clubId 社团ID（可选）
   * @return 招新信息
   */
  @GetMapping("/site/recruitment")
  public Result<ResponseRecruitmentVO> getRecruitment(
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
  public Result<ResponseRecruitmentDetailVO> getRecruitmentDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer campaignId) {
    return siteService.getRecruitmentDetail(campaignId);
  }

  /**
   * 获取报名表单详情
   *
   * @param formId 表单ID
   * @return 表单详情
   */
  @GetMapping("/site/forms/{formId}")
  public Result<ResponseSiteFormDetailVO> getFormDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer formId) {
    return siteService.getFormDetail(formId);
  }

  /**
   * 获取当前用户的报名进度
   *
   * @return 用户进度信息
   */
  @GetMapping("/site/progress")
  public ResponseEntity<Result<ResponseSiteProgressVO>> getMyProgress() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(siteService.getMyProgress());
  }

  /**
   * 获取当前用户的账号激活页数据
   *
   * <p>返回用户成员身份、所属部门、部门部长、社长副社长及社团介绍，供激活页展示。
   * 响应包含姓名、学号、手机号等个人资料，必须声明 no-store，禁止任何共享缓存
   * （含 Nginx proxy_cache、CDN、浏览器磁盘缓存）保存。
   *
   * @return 激活页数据
   */
  @GetMapping("/site/activation")
  public ResponseEntity<Result<ResponseActivationVO>> getMyActivationInfo() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(siteService.getMyActivationInfo());
  }

  /**
   * 查询注册功能是否开启
   *
   * @return 注册是否开启
   */
  @GetMapping("/site/register-enabled")
  public Result<Boolean> getRegisterEnabled() {
    return siteService.getRegisterEnabled();
  }

  /**
   * 查询激活页引导流程是否启用
   *
   * @return 激活页是否启用
   */
  @GetMapping("/site/activation-enabled")
  public Result<Boolean> getActivationEnabled() {
    return siteService.getActivationEnabled();
  }
}
