package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.SiteFormMapper;
import edu.jmi.openatom.server.openatomsystem.service.SiteFormService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteFormServiceImpl implements SiteFormService {
  private static final List<String> STATUSES =
      List.of("draft", "published", "open", "closed", "archived");

  private final SiteFormMapper siteFormMapper;
  private final ClubMapper clubMapper;

  @Override
  public ApiResponse<List<SiteForm>> listByClub(Integer clubId) {
    if (clubId == null) {
      return ApiResponse.error(400, "clubId不能为空");
    }
    if (clubMapper.selectById(clubId) == null) {
      return ApiResponse.error(404, "社团不存在");
    }
    return ApiResponse.success(
        siteFormMapper.selectList(
            new LambdaQueryWrapper<SiteForm>()
                .eq(SiteForm::getClubId, clubId)
                .orderByDesc(SiteForm::getId)));
  }

  @Override
  public ApiResponse<String> create(Integer clubId, RequestCreateSiteFormDTO request) {
    if (clubId == null) {
      return ApiResponse.error(400, "clubId不能为空");
    }
    Club club = clubMapper.selectById(clubId);
    if (club == null) {
      return ApiResponse.error(404, "社团不存在");
    }
    String status = request.getStatus() == null ? "draft" : request.getStatus();
    if (!STATUSES.contains(status)) {
      return ApiResponse.error(400, "招新计划状态不合法");
    }

    SiteForm form =
        SiteForm.builder()
            .clubId(clubId)
            .name(request.getName())
            .startAt(Times.parseTimestamp(request.getStartAt()))
            .endAt(Times.parseTimestamp(request.getEndAt()))
            .loginRequired(Boolean.TRUE.equals(request.getLoginRequired()))
            .formSchema(Jsons.stringify(request.getFormSchema()))
            .status(status)
            .build();
    int row = siteFormMapper.insert(form);
    return row > 0 ? ApiResponse.success("表单创建成功") : ApiResponse.error("表单创建失败");
  }

  @Override
  public ApiResponse<SiteForm> detail(Integer formId) {
    SiteForm form = findSiteForm(formId);
    return form == null ? ApiResponse.error(404, "表单不存在") : ApiResponse.success(form);
  }

  @Override
  public ApiResponse<String> update(Integer formId, RequestUpdateSiteFormDTO request) {
    SiteForm form = findSiteForm(formId);
    if (form == null) {
      return ApiResponse.error(404, "表单不存在");
    }
    if (request.getName() != null) {
      form.setName(request.getName());
    }
    if (request.getStartAt() != null) {
      form.setStartAt(Times.parseTimestamp(request.getStartAt()));
    }
    if (request.getEndAt() != null) {
      form.setEndAt(Times.parseTimestamp(request.getEndAt()));
    }
    if (request.getLoginRequired() != null) {
      form.setLoginRequired(request.getLoginRequired());
    }
    if (request.getFormSchema() != null) {
      form.setFormSchema(Jsons.stringify(request.getFormSchema()));
    }
    if (request.getStatus() != null) {
      if (!STATUSES.contains(request.getStatus())) {
        return ApiResponse.error(400, "表单状态不合法");
      }
      form.setStatus(request.getStatus());
    }
    int row = siteFormMapper.updateById(form);
    return row > 0 ? ApiResponse.success("表单更新成功") : ApiResponse.error("表单更新失败");
  }

  @Override
  public ApiResponse<String> publish(Integer formId) {
    return updateStatus(formId, "open", "表单发布成功");
  }

  @Override
  public ApiResponse<String> close(Integer formId) {
    return updateStatus(formId, "closed", "表单关闭成功");
  }

  private ApiResponse<String> updateStatus(Integer formId, String status, String message) {
    SiteForm form = findSiteForm(formId);
    if (form == null) {
      return ApiResponse.error(404, "表单不存在");
    }
    form.setStatus(status);
    int row = siteFormMapper.updateById(form);
    return row > 0 ? ApiResponse.success(message) : ApiResponse.error("表单状态更新失败");
  }

  private SiteForm findSiteForm(Integer formId) {
    return formId == null ? null : siteFormMapper.selectById(formId);
  }
}
