package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.SiteFormMapper;
import edu.jmi.openatom.server.openatomsystem.service.SiteFormService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站点表单管理实现类
 *
 * <p>负责前端展示表单的创建, 更新, 查询, 发布和关闭等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class SiteFormServiceImpl implements SiteFormService {
  private static final List<String> STATUSES = List.of("draft", "published", "open", "closed", "archived");

  private final SiteFormMapper siteFormMapper;
  private final ClubMapper clubMapper;

  @Override
  public Result<List<SiteForm>> listByClub(Integer clubId) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (clubMapper.selectById(clubId) == null) return Result.error(404, "社团不存在");
    return Result.success(siteFormMapper.selectByClubIdOrdered(clubId));
  }

  @Override
  public Result<String> create(Integer clubId, RequestCreateSiteFormDTO request) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    Club club = clubMapper.selectById(clubId);
    if (club == null) return Result.error(404, "社团不存在");
    String status = request.getStatus() == null ? "draft" : request.getStatus();
    if (!STATUSES.contains(status)) return Result.error(400, "招新计划状态不合法");
    SiteForm form = SiteForm.builder().clubId(clubId).name(request.getName())
        .startAt(Times.parseTimestamp(request.getStartAt())).endAt(Times.parseTimestamp(request.getEndAt()))
        .loginRequired(Boolean.TRUE.equals(request.getLoginRequired()))
        .formSchema(Jsons.stringify(request.getFormSchema())).status(status).build();
    int row = siteFormMapper.insert(form);
    return row > 0 ? Result.success("表单创建成功") : Result.error("表单创建失败");
  }

  @Override
  public Result<SiteForm> detail(Integer formId) {
    SiteForm form = findSiteForm(formId);
    return form == null ? Result.error(404, "表单不存在") : Result.success(form);
  }

  @Override
  public Result<String> update(Integer formId, RequestUpdateSiteFormDTO request) {
    SiteForm form = findSiteForm(formId);
    if (form == null) return Result.error(404, "表单不存在");
    if (request.getName() != null) form.setName(request.getName());
    if (request.getStartAt() != null) form.setStartAt(Times.parseTimestamp(request.getStartAt()));
    if (request.getEndAt() != null) form.setEndAt(Times.parseTimestamp(request.getEndAt()));
    if (request.getLoginRequired() != null) form.setLoginRequired(request.getLoginRequired());
    if (request.getFormSchema() != null) form.setFormSchema(Jsons.stringify(request.getFormSchema()));
    if (request.getStatus() != null) {
      if (!STATUSES.contains(request.getStatus())) return Result.error(400, "表单状态不合法");
      form.setStatus(request.getStatus());
    }
    int row = siteFormMapper.updateById(form);
    return row > 0 ? Result.success("表单更新成功") : Result.error("表单更新失败");
  }

  @Override
  public Result<String> publish(Integer formId) { return updateStatus(formId, "open", "表单发布成功"); }

  @Override
  public Result<String> close(Integer formId) { return updateStatus(formId, "closed", "表单关闭成功"); }

  private Result<String> updateStatus(Integer formId, String status, String message) {
    SiteForm form = findSiteForm(formId);
    if (form == null) return Result.error(404, "表单不存在");
    form.setStatus(status);
    int row = siteFormMapper.updateById(form);
    return row > 0 ? Result.success(message) : Result.error("表单状态更新失败");
  }

  private SiteForm findSiteForm(Integer formId) { return formId == null ? null : siteFormMapper.selectById(formId); }
}
