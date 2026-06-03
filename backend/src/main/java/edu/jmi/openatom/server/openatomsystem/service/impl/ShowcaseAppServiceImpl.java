package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveShowcaseAppDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ShowcaseApp;
import edu.jmi.openatom.server.openatomsystem.mapper.ShowcaseAppMapper;
import edu.jmi.openatom.server.openatomsystem.service.ShowcaseAppService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用展示服务实现
 *
 * <p>负责应用条目的创建、公开展示、发布状态和基础字段维护
 */
@Service
@RequiredArgsConstructor
public class ShowcaseAppServiceImpl implements ShowcaseAppService {
  private static final String STATUS_DRAFT = "draft";
  private static final String STATUS_PUBLISHED = "published";
  private static final String STATUS_HIDDEN = "hidden";

  private final ShowcaseAppMapper showcaseAppMapper;

  @Override
  public Result<List<ShowcaseApp>> publicList(String keyword, Boolean openSource) {
    return Result.success(showcaseAppMapper.selectPublished(trimToNull(keyword), openSource));
  }

  @Override
  public Result<ShowcaseApp> publicDetail(Integer appId) {
    ShowcaseApp app = showcaseAppMapper.selectPublishedById(appId);
    return app == null ? Result.error(404, "应用不存在或未发布") : Result.success(app);
  }

  @Override
  public Result<PageDataVO<ShowcaseApp>> adminList(
      String keyword, String status, Boolean openSource, Long page, Long pageSize) {
    Page<ShowcaseApp> result =
        showcaseAppMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            trimToNull(keyword),
            normalizeStatus(status),
            openSource);
    return Result.success(toPage(result));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ShowcaseApp> create(RequestSaveShowcaseAppDTO request, Integer operatorId) {
    if (request == null) return Result.error(400, "应用信息不能为空");
    ShowcaseApp app = new ShowcaseApp();
    applyRequest(app, request);
    app.setStatus(defaultStatus(request.getStatus()));
    app.setCreatedBy(operatorId);
    app.setUpdatedBy(operatorId);
    app.setCreatedAt(Times.now());
    showcaseAppMapper.insert(app);
    return Result.success(app, "应用已创建");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ShowcaseApp> update(
      Integer appId, RequestSaveShowcaseAppDTO request, Integer operatorId) {
    ShowcaseApp app = appId == null ? null : showcaseAppMapper.selectById(appId);
    if (app == null) return Result.error(404, "应用不存在");
    if (request == null) return Result.error(400, "应用信息不能为空");
    applyRequest(app, request);
    app.setStatus(defaultStatus(request.getStatus()));
    app.setUpdatedBy(operatorId);
    showcaseAppMapper.updateById(app);
    return Result.success(app, "应用已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ShowcaseApp> updateStatus(Integer appId, String status, Integer operatorId) {
    ShowcaseApp app = appId == null ? null : showcaseAppMapper.selectById(appId);
    if (app == null) return Result.error(404, "应用不存在");
    app.setStatus(defaultStatus(status));
    app.setUpdatedBy(operatorId);
    showcaseAppMapper.updateById(app);
    return Result.success(app, "应用状态已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> delete(Integer appId) {
    ShowcaseApp app = appId == null ? null : showcaseAppMapper.selectById(appId);
    if (app == null) return Result.error(404, "应用不存在");
    showcaseAppMapper.deleteById(appId);
    return Result.success("应用已删除");
  }

  private void applyRequest(ShowcaseApp app, RequestSaveShowcaseAppDTO request) {
    app.setName(required(request.getName(), "应用名称不能为空"));
    app.setSummary(trimToNull(request.getSummary()));
    app.setCoverUrl(trimToNull(request.getCoverUrl()));
    app.setOpenSource(Boolean.TRUE.equals(request.getOpenSource()));
    app.setGithubUrl(trimToNull(request.getGithubUrl()));
    app.setGiteeUrl(trimToNull(request.getGiteeUrl()));
    app.setDeveloper(trimToNull(request.getDeveloper()));
    app.setOwner(trimToNull(request.getOwner()));
    app.setLicenseName(trimToNull(request.getLicenseName()));
    app.setVersion(trimToNull(request.getVersion()));
    app.setAppStatus(trimToNull(request.getAppStatus()));
    app.setDownloadUrl(trimToNull(request.getDownloadUrl()));
    app.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    if (!Boolean.TRUE.equals(app.getOpenSource())) {
      app.setGithubUrl(null);
      app.setGiteeUrl(null);
    }
  }

  private PageDataVO<ShowcaseApp> toPage(Page<ShowcaseApp> page) {
    return PageDataVO.<ShowcaseApp>builder()
        .list(page.getRecords())
        .page(page.getCurrent())
        .pageSize(page.getSize())
        .total(page.getTotal())
        .build();
  }

  private String defaultStatus(String status) {
    String normalized = normalizeStatus(status);
    if (normalized == null) return STATUS_DRAFT;
    if (STATUS_DRAFT.equals(normalized)
        || STATUS_PUBLISHED.equals(normalized)
        || STATUS_HIDDEN.equals(normalized)) {
      return normalized;
    }
    throw new IllegalArgumentException("应用状态只能是 draft、published 或 hidden");
  }

  private String normalizeStatus(String status) {
    String value = trimToNull(status);
    return value == null ? null : value.toLowerCase();
  }

  private String required(String value, String message) {
    String trimmed = trimToNull(value);
    if (trimmed == null) throw new IllegalArgumentException(message);
    return trimmed;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
