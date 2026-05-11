package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import edu.jmi.openatom.server.openatomsystem.service.SiteFormService;
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
 * 站点表单管理控制器
 *
 * <p>提供招新报名表单的列表查询, 创建, 详情, 更新, 发布及关闭等功能
 */
@RestController
@RequiredArgsConstructor
public class SiteFormController {
  private final SiteFormService siteFormService;

  /**
   * 获取社团的站点表单列表
   *
   * @param clubId 社团ID
   * @return 表单列表
   */
  @GetMapping("/clubs/{clubId}/site-forms")
  @SaCheckPermission("site-form:list")
  public ApiResponse<List<SiteForm>> list(@PathVariable Integer clubId) {
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
  public ApiResponse<String> create(
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
  public ApiResponse<SiteForm> detail(@PathVariable Integer formId) {
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
  public ApiResponse<String> update(
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
  public ApiResponse<String> publish(@PathVariable Integer formId) {
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
  public ApiResponse<String> close(@PathVariable Integer formId) {
    return siteFormService.close(formId);
  }
}
