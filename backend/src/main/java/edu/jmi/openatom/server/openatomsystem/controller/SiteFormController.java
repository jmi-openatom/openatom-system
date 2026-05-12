package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import edu.jmi.openatom.server.openatomsystem.service.SiteFormService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点表单管理控制器
 *
 * <p>提供招新报名表单的列表查询, 创建, 详情, 更新, 发布及关闭等功能
 */
@RestController
@RequiredArgsConstructor
public class SiteFormController {
  private final SiteFormService siteFormService;

  @Value("${app.miniapp.app-id:wx8c6b52ab95da0938}")
  private String miniappAppId;

  /**
   * 获取社团的站点表单列表
   *
   * @param clubId 社团ID
   * @return 表单列表
   */
  @GetMapping("/clubs/{clubId}/site-forms")
  @SaCheckPermission("site-form:list")
  public Result<List<SiteForm>> list(@PathVariable Integer clubId) {
    return siteFormService.listByClub(clubId);
  }

  /**
   * 创建站点表单
   *
   * @param clubId 社团ID
   * @param request 创建表单请求参数
   * @return 创建结果
   */
  @PostMapping("/clubs/{clubId}/site-forms")
  @SaCheckPermission("site-form:create")
  public Result<String> create(
      @PathVariable Integer clubId, @Valid @RequestBody RequestCreateSiteFormDTO request) {
    return siteFormService.create(clubId, request);
  }

  /**
   * 获取表单详情
   *
   * @param formId 表单ID
   * @return 表单详情
   */
  @GetMapping("/site-forms/{formId}")
  @SaCheckPermission("site-form:detail")
  public Result<SiteForm> detail(@PathVariable Integer formId) {
    return siteFormService.detail(formId);
  }

  /**
   * 更新站点表单
   *
   * @param formId 表单ID
   * @param request 更新表单请求参数
   * @return 更新结果
   */
  @PatchMapping("/site-forms/{formId}")
  @SaCheckPermission("site-form:update")
  public Result<String> update(
      @PathVariable Integer formId, @RequestBody RequestUpdateSiteFormDTO request) {
    return siteFormService.update(formId, request);
  }

  /**
   * 发布站点表单
   *
   * @param formId 表单ID
   * @return 发布结果
   */
  @PostMapping("/site-forms/{formId}/publish")
  @SaCheckPermission("site-form:update")
  public Result<String> publish(@PathVariable Integer formId) {
    return siteFormService.publish(formId);
  }

  /**
   * 关闭站点表单
   *
   * @param formId 表单ID
   * @return 关闭结果
   */
  @PostMapping("/site-forms/{formId}/close")
  @SaCheckPermission("site-form:update")
  public Result<String> close(@PathVariable Integer formId) {
    return siteFormService.close(formId);
  }

  /**
   * 获取表单的分享信息（小程序链接）
   *
   * @param formId 表单ID
   * @return 分享信息（包含小程序路径）
   */
  @GetMapping("/site-forms/{formId}/share-info")
  @SaCheckPermission("site-form:detail")
  public Result<Map<String, Object>> shareInfo(@PathVariable Integer formId) {
    SiteForm form = siteFormService.detail(formId).getData();
    if (form == null) return Result.error(404, "表单不存在");
    Map<String, Object> info = new HashMap<>();
    info.put("formId", form.getId());
    info.put("formName", form.getName());
    info.put("formStatus", form.getStatus());
    info.put("miniappPath", "pages/forms/index?id=" + form.getId());
    info.put("miniappAppId", miniappAppId);
    return Result.success(info);
  }
}
