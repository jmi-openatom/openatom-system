package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import edu.jmi.openatom.server.openatomsystem.service.RecruitmentCampaignService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 招新活动管理控制器
 *
 * <p>提供招新活动的列表查询, 创建, 详情, 更新, 发布及关闭等功能
 */
@RestController
@RequiredArgsConstructor
public class RecruitmentCampaignController {
  private final RecruitmentCampaignService recruitmentCampaignService;

  /**
   * 获取社团的招新活动列表
   *
   * @param clubId 社团ID
   * @return 招新活动列表
   */
  @GetMapping("/clubs/{clubId}/recruitment-campaigns")
  @SaCheckPermission("recruitment-campaign:list")
  public ApiResponse<List<RecruitmentCampaign>> list(@PathVariable Integer clubId) {
    return recruitmentCampaignService.listByClub(clubId);
  }

  /**
   * 创建招新活动
   *
   * @param clubId 社团ID
   * @param request 创建招新活动请求参数
   * @return 创建结果
   */
  @PostMapping("/clubs/{clubId}/recruitment-campaigns")
  @SaCheckPermission("recruitment-campaign:create")
  public ApiResponse<String> create(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestCreateRecruitmentCampaignDTO request) {
    return recruitmentCampaignService.create(clubId, request);
  }

  /**
   * 获取招新活动详情
   *
   * @param campaignId 招新活动ID
   * @return 招新活动详情
   */
  @GetMapping("/recruitment-campaigns/{campaignId}")
  @SaCheckPermission("recruitment-campaign:detail")
  public ApiResponse<RecruitmentCampaign> detail(@PathVariable Integer campaignId) {
    return recruitmentCampaignService.detail(campaignId);
  }

  /**
   * 更新招新活动
   *
   * @param campaignId 招新活动ID
   * @param request 更新招新活动请求参数
   * @return 更新结果
   */
  @PatchMapping("/recruitment-campaigns/{campaignId}")
  @SaCheckPermission("recruitment-campaign:update")
  public ApiResponse<String> update(
      @PathVariable Integer campaignId,
      @Valid @RequestBody RequestUpdateRecruitmentCampaignDTO request) {
    return recruitmentCampaignService.update(campaignId, request);
  }

  /**
   * 发布招新活动
   *
   * @param campaignId 招新活动ID
   * @return 发布结果
   */
  @PostMapping("/recruitment-campaigns/{campaignId}/publish")
  @SaCheckPermission("recruitment-campaign:publish")
  public ApiResponse<String> publish(@PathVariable Integer campaignId) {
    return recruitmentCampaignService.publish(campaignId);
  }

  /**
   * 关闭招新活动
   *
   * @param campaignId 招新活动ID
   * @return 关闭结果
   */
  @PostMapping("/recruitment-campaigns/{campaignId}/close")
  @SaCheckPermission("recruitment-campaign:close")
  public ApiResponse<String> close(@PathVariable Integer campaignId) {
    return recruitmentCampaignService.close(campaignId);
  }
}
