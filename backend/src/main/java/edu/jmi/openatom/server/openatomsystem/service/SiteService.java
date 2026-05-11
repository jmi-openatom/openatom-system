package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseClubHomeDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseRecruitmentDetailDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteProgressDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteFormsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteFormDetailDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseRecruitmentDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import java.util.List;

/**
 * 站点前端展示服务接口
 *
 * <p>定义社团主页, 活动列表, 活动详情, 公开社团列表, 公开表单, 招新信息, 招新详情, 表单详情, 我的进度以及注册开关查询等对外展示业务操作
 */
public interface SiteService {
  ApiResponse<ResponseClubHomeDTO> getClubHome(String clubCode);

  ApiResponse<List<ClubActivity>> getActivities();

  ApiResponse<ClubActivity> getActivityDetail(Integer activityId);

  ApiResponse<List<Club>> getPublicClubs();

  ApiResponse<ResponseSiteFormsDTO> getPublicForms(Integer clubId);

  ApiResponse<ResponseRecruitmentDTO> getRecruitment(Integer clubId);

  ApiResponse<ResponseRecruitmentDetailDTO> getRecruitmentDetail(Integer campaignId);

  ApiResponse<ResponseSiteFormDetailDTO> getFormDetail(Integer campaignId);

  ApiResponse<ResponseSiteProgressDTO> getMyProgress();

  ApiResponse<Boolean> getRegisterEnabled();
}
