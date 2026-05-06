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

@RestController
@RequiredArgsConstructor
public class SiteController {
  private final SiteService siteService;

  @GetMapping("/site/club-home")
  public ApiResponse<ResponseClubHomeDTO> getClubHome(
      @RequestParam(required = false) String clubCode) {
    return siteService.getClubHome(clubCode);
  }

  @GetMapping("/site/activities")
  public ApiResponse<List<ClubActivity>> getActivities() {
    return siteService.getActivities();
  }

  @GetMapping("/site/activities/{activityId}")
  public ApiResponse<ClubActivity> getActivityDetail(@org.springframework.web.bind.annotation.PathVariable Integer activityId) {
    return siteService.getActivityDetail(activityId);
  }

  @GetMapping("/site/recruitment")
  public ApiResponse<ResponseRecruitmentDTO> getRecruitment(
      @RequestParam(required = false) Integer clubId) {
    return siteService.getRecruitment(clubId);
  }

  @GetMapping("/site/recruitment/{campaignId}")
  public ApiResponse<ResponseRecruitmentDetailDTO> getRecruitmentDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer campaignId) {
    return siteService.getRecruitmentDetail(campaignId);
  }

  @GetMapping("/site/forms/{campaignId}")
  public ApiResponse<ResponseSiteFormDetailDTO> getFormDetail(
      @org.springframework.web.bind.annotation.PathVariable Integer campaignId) {
    return siteService.getFormDetail(campaignId);
  }

  @GetMapping("/site/progress")
  public ApiResponse<ResponseSiteProgressDTO> getMyProgress() {
    return siteService.getMyProgress();
  }

  @GetMapping("/site/register-enabled")
  public ApiResponse<Boolean> getRegisterEnabled() {
    return siteService.getRegisterEnabled();
  }
}
