package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveShowcaseAppDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ShowcaseApp;
import edu.jmi.openatom.server.openatomsystem.service.ShowcaseAppService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应用展示控制器
 *
 * <p>提供前台应用展示和后台应用条目管理接口
 */
@RestController
@RequiredArgsConstructor
public class ShowcaseAppController {
  private final ShowcaseAppService showcaseAppService;

  @GetMapping("/site/apps")
  public Result<List<ShowcaseApp>> publicList(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean openSource) {
    return showcaseAppService.publicList(keyword, openSource);
  }

  @GetMapping("/site/apps/{appId}")
  public Result<ShowcaseApp> publicDetail(@PathVariable Integer appId) {
    return showcaseAppService.publicDetail(appId);
  }

  @GetMapping("/showcase-apps")
  @SaCheckPermission("showcase-app:list")
  public Result<PageDataVO<ShowcaseApp>> adminList(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean openSource,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return showcaseAppService.adminList(keyword, status, openSource, page, pageSize);
  }

  @PostMapping("/showcase-apps")
  @SaCheckPermission("showcase-app:manage")
  public Result<ShowcaseApp> create(@Valid @RequestBody RequestSaveShowcaseAppDTO request) {
    return showcaseAppService.create(request, currentUserId());
  }

  @PatchMapping("/showcase-apps/{appId}")
  @SaCheckPermission("showcase-app:manage")
  public Result<ShowcaseApp> update(
      @PathVariable Integer appId, @Valid @RequestBody RequestSaveShowcaseAppDTO request) {
    return showcaseAppService.update(appId, request, currentUserId());
  }

  @PatchMapping("/showcase-apps/{appId}/status")
  @SaCheckPermission("showcase-app:manage")
  public Result<ShowcaseApp> updateStatus(
      @PathVariable Integer appId, @RequestParam String status) {
    return showcaseAppService.updateStatus(appId, status, currentUserId());
  }

  @DeleteMapping("/showcase-apps/{appId}")
  @SaCheckPermission("showcase-app:delete")
  public Result<String> delete(@PathVariable Integer appId) {
    return showcaseAppService.delete(appId);
  }

  private Integer currentUserId() {
    return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
  }
}
