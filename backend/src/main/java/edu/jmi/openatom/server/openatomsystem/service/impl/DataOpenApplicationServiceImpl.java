package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.RequestDataOpenApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewDataOpenApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.DataOpenApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.DataOpenApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.DataOpenApplicationService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseDataOpenApplicationVO;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataOpenApplicationServiceImpl implements DataOpenApplicationService {
  private static final String STATUS_PENDING = "pending";
  private static final String STATUS_APPROVED = "approved";
  private static final String STATUS_REJECTED = "rejected";
  private static final String ENDPOINT_PUBLIC_LOGIN = "public_login";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final DataOpenApplicationMapper dataOpenApplicationMapper;
  private final UserMapper userMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseDataOpenApplicationVO> submit(RequestDataOpenApplicationDTO request) {
    if (request == null) return Result.error(400, "申请内容不能为空");
    DataOpenApplication application =
        DataOpenApplication.builder()
            .appName(trimRequired(request.getAppName(), "应用名称不能为空"))
            .applicantName(trimRequired(request.getApplicantName(), "申请人不能为空"))
            .applicantContact(trimRequired(request.getApplicantContact(), "联系方式不能为空"))
            .organization(trimToNull(request.getOrganization()))
            .purpose(trimRequired(request.getPurpose(), "使用场景不能为空"))
            .endpointScope(ENDPOINT_PUBLIC_LOGIN)
            .status(STATUS_PENDING)
            .callCount(0)
            .createdAt(Times.now())
            .build();
    dataOpenApplicationMapper.insert(application);
    return Result.success(toResponse(application, null), "申请已提交，等待管理员审核");
  }

  @Override
  public Result<PageDataVO<ResponseDataOpenApplicationVO>> adminList(
      String keyword, String status, Long page, Long pageSize) {
    Page<DataOpenApplication> applicationPage =
        dataOpenApplicationMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            keyword,
            normalizeStatus(status));
    return Result.success(toPage(applicationPage));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseDataOpenApplicationVO> review(
      Integer applicationId, RequestReviewDataOpenApplicationDTO request, Integer reviewerId) {
    DataOpenApplication application =
        applicationId == null ? null : dataOpenApplicationMapper.selectById(applicationId);
    if (application == null) return Result.error(404, "开放平台申请不存在");
    if (request == null) return Result.error(400, "审核内容不能为空");
    String status = normalizeStatus(request.getStatus());
    if (!STATUS_APPROVED.equals(status) && !STATUS_REJECTED.equals(status)) {
      return Result.error(400, "审核状态只能是 approved 或 rejected");
    }

    application.setStatus(status);
    application.setReviewComment(trimToNull(request.getReviewComment()));
    application.setReviewedBy(reviewerId);
    application.setReviewedAt(Times.now());
    if (STATUS_APPROVED.equals(status) && trimToNull(application.getApiKey()) == null) {
      application.setApiKey(generateUniqueApiKey());
    }
    if (STATUS_REJECTED.equals(status)) {
      application.setApiKey(null);
    }
    dataOpenApplicationMapper.updateById(application);
    User reviewer = reviewerId == null ? null : userMapper.selectById(reviewerId);
    return Result.success(toResponse(application, reviewer), "审核已完成");
  }

  @Override
  public Result<DataOpenApplication> validateApiKey(String apiKey) {
    String key = trimToNull(apiKey);
    if (key == null) return Result.error(401, "数据开放平台密钥不能为空");
    DataOpenApplication application = dataOpenApplicationMapper.selectByApiKey(key);
    if (application == null || !STATUS_APPROVED.equals(application.getStatus())) {
      return Result.error(403, "数据开放平台密钥无效或未通过审核");
    }
    if (!ENDPOINT_PUBLIC_LOGIN.equals(application.getEndpointScope())) {
      return Result.error(403, "当前密钥无权调用该接口");
    }
    return Result.success(application);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void recordUsage(DataOpenApplication application) {
    if (application == null || application.getId() == null) return;
    DataOpenApplication latest = dataOpenApplicationMapper.selectById(application.getId());
    if (latest == null) return;
    latest.setLastUsedAt(Times.now());
    latest.setCallCount((latest.getCallCount() == null ? 0 : latest.getCallCount()) + 1);
    dataOpenApplicationMapper.updateById(latest);
  }

  private PageDataVO<ResponseDataOpenApplicationVO> toPage(Page<DataOpenApplication> page) {
    List<DataOpenApplication> applications = page.getRecords();
    Map<Integer, User> reviewers = reviewerMap(applications);
    return PageDataVO.<ResponseDataOpenApplicationVO>builder()
        .list(applications.stream().map(item -> toResponse(item, reviewerOf(item, reviewers))).toList())
        .page(page.getCurrent())
        .pageSize(page.getSize())
        .total(page.getTotal())
        .build();
  }

  private User reviewerOf(DataOpenApplication application, Map<Integer, User> reviewers) {
    Integer reviewerId = application == null ? null : application.getReviewedBy();
    return reviewerId == null ? null : reviewers.get(reviewerId);
  }

  private Map<Integer, User> reviewerMap(List<DataOpenApplication> applications) {
    List<Integer> userIds =
        applications.stream()
            .map(DataOpenApplication::getReviewedBy)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (userIds.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
  }

  private ResponseDataOpenApplicationVO toResponse(DataOpenApplication application, User reviewer) {
    if (application == null) return null;
    return ResponseDataOpenApplicationVO.builder()
        .id(application.getId())
        .appName(application.getAppName())
        .applicantName(application.getApplicantName())
        .applicantContact(application.getApplicantContact())
        .organization(application.getOrganization())
        .purpose(application.getPurpose())
        .endpointScope(application.getEndpointScope())
        .status(application.getStatus())
        .apiKey(application.getApiKey())
        .reviewComment(application.getReviewComment())
        .reviewedBy(application.getReviewedBy())
        .reviewerName(displayName(reviewer))
        .reviewedAt(application.getReviewedAt())
        .lastUsedAt(application.getLastUsedAt())
        .callCount(application.getCallCount() == null ? 0 : application.getCallCount())
        .createdAt(application.getCreatedAt())
        .updatedAt(application.getUpdatedAt())
        .build();
  }

  private String generateUniqueApiKey() {
    for (int i = 0; i < 8; i++) {
      String key = "oa_" + randomToken();
      if (dataOpenApplicationMapper.selectByApiKey(key) == null) return key;
    }
    throw new IllegalStateException("生成开放平台密钥失败，请重试");
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String displayName(User user) {
    if (user == null) return null;
    if (trimToNull(user.getRealName()) != null) return user.getRealName().trim();
    return user.getUserName();
  }

  private String normalizeStatus(String status) {
    String value = trimToNull(status);
    return value == null ? null : value.toLowerCase();
  }

  private String trimRequired(String value, String message) {
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
