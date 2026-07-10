package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseActivationVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubHomeVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseRecruitmentDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteProgressVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteFormsVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteFormDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseRecruitmentVO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import java.util.List;

/**
 * 站点前端展示服务接口
 *
 * <p>定义社团主页, 活动列表, 活动详情, 公开社团列表, 公开表单, 招新信息, 招新详情, 表单详情, 我的进度以及注册开关查询等对外展示业务操作
 */
public interface SiteService {
  Result<ResponseClubHomeVO> getClubHome(String clubCode);

  Result<List<ClubActivity>> getActivities();

  Result<ClubActivity> getActivityDetail(Integer activityId);

  Result<List<Club>> getPublicClubs();

  Result<ResponseSiteFormsVO> getPublicForms(Integer clubId);

  Result<ResponseRecruitmentVO> getRecruitment(Integer clubId);

  Result<ResponseRecruitmentDetailVO> getRecruitmentDetail(Integer campaignId);

  Result<ResponseSiteFormDetailVO> getFormDetail(Integer campaignId);

  Result<ResponseSiteProgressVO> getMyProgress();

  Result<ResponseActivationVO> getMyActivationInfo();

  Result<Boolean> getRegisterEnabled();

  Result<Boolean> getActivationEnabled();
}
