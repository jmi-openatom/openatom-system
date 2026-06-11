package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBotCreateLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.LeaveApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.LeaveApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
import edu.jmi.openatom.server.openatomsystem.service.LeaveApplicationService;
import edu.jmi.openatom.server.openatomsystem.service.impl.LeaveAttachmentStorageServiceImpl.StoredLeaveAttachment;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLeaveApplicationVO;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveApplicationServiceImpl implements LeaveApplicationService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";

  private final ClubMapper clubMapper;
  private final UserMapper userMapper;
  private final LeaveApplicationMapper leaveApplicationMapper;
  private final LeaveAttachmentStorageServiceImpl leaveAttachmentStorageService;
  private final CheckInService checkInService;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Value("${app.bot.leave-review-callback-url:http://astrbot:6198/openatom/leave-review}")
  private String leaveReviewCallbackUrl;

  @Value("${app.bot.callback-token:}")
  private String botCallbackToken;

  @Override
  public Result<List<ResponseLeaveApplicationVO>> list(String status) {
    return Result.success(leaveApplicationMapper.selectAllOrdered(status).stream().map(this::toVO).toList());
  }

  @Override
  public Result<List<ResponseLeaveApplicationVO>> mine() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    Integer userId = StpUtil.getLoginIdAsInt();
    return Result.success(leaveApplicationMapper.selectByUserOrdered(userId).stream().map(this::toVO).toList());
  }

  @Override
  public Result<ResponseLeaveApplicationVO> detail(Integer leaveApplicationId) {
    LeaveApplication application = find(leaveApplicationId);
    if (application == null) return Result.error(404, "请假申请不存在");
    if (!canView(application)) return Result.error(403, "无权查看该请假申请");
    return Result.success(toVO(application));
  }

  @Override
  public Result<ResponseLeaveApplicationVO> botDetailByQq(Integer leaveApplicationId, String qqOpenid) {
    String normalizedQqOpenid = qqOpenid == null ? "" : qqOpenid.trim();
    User user = normalizedQqOpenid.isBlank() ? null : userMapper.selectByQqOpenid(normalizedQqOpenid);
    if (user == null) {
      return Result.error(401, "当前QQ未绑定系统账号，请先在网页个人中心生成绑定码并发送给机器人绑定");
    }
    LeaveApplication application = find(leaveApplicationId);
    if (application == null) return Result.error(404, "请假申请不存在");
    if (!user.getId().equals(application.getUserId())) return Result.error(403, "无权查看该请假申请");
    return Result.success(toVO(application));
  }

  @Override
  public Result<Integer> create(RequestCreateLeaveApplicationDTO request) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    return createForUser(StpUtil.getLoginIdAsInt(), request.getTitle(), request.getReason(),
        request.getStartAt(), request.getEndAt(), request.getAttachments());
  }

  @Override
  public Result<Integer> createByQq(RequestBotCreateLeaveApplicationDTO request) {
    String qqOpenid = request.getQqOpenid() == null ? "" : request.getQqOpenid().trim();
    User user = qqOpenid.isBlank() ? null : userMapper.selectByQqOpenid(qqOpenid);
    if (user == null) {
      return Result.error(401, "当前QQ未绑定系统账号，请先在网页个人中心生成绑定码并发送给机器人绑定");
    }
    return createForUser(user.getId(), request.getTitle(), request.getReason(),
        request.getStartAt(), request.getEndAt(), request.getAttachments(),
        request.getBotNotifyOrigin(), request.getBotNotifyUserId());
  }

  private Result<Integer> createForUser(
      Integer userId,
      String title,
      String reason,
      String startAt,
      String endAt,
      List<Map<String, Object>> rawAttachments) {
    return createForUser(userId, title, reason, startAt, endAt, rawAttachments, null, null);
  }

  private Result<Integer> createForUser(
      Integer userId,
      String title,
      String reason,
      String startAt,
      String endAt,
      List<Map<String, Object>> rawAttachments,
      String botNotifyOrigin,
      String botNotifyUserId) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    List<Map<String, Object>> attachments;
    try {
      attachments = sanitizeAttachments(rawAttachments, userId);
    } catch (IOException | IllegalArgumentException e) {
      return Result.error(400, e.getMessage());
    }
    if (attachments.isEmpty()) return Result.error(400, "请上传请假附件");
    LeaveApplication application =
        LeaveApplication.builder()
            .clubId(club.getId())
            .userId(userId)
            .title(title.trim())
            .reason(reason.trim())
            .startAt(Times.parseTimestamp(startAt))
            .endAt(Times.parseTimestamp(endAt))
            .attachments(Jsons.stringify(attachments))
            .status("submitted")
            .botNotifyOrigin(trimToNull(botNotifyOrigin))
            .botNotifyUserId(trimToNull(botNotifyUserId))
            .build();
    leaveApplicationMapper.insert(application);
    return Result.success(application.getId(), "请假申请已提交");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> review(Integer leaveApplicationId, RequestReviewLeaveApplicationDTO request) {
    LeaveApplication application = find(leaveApplicationId);
    if (application == null) return Result.error(404, "请假申请不存在");
    if (!"submitted".equals(application.getStatus())) return Result.error(422, "该申请已审批");
    String action = request.getAction() == null ? "" : request.getAction().trim();
    if (!"approve".equals(action) && !"reject".equals(action)) return Result.error(400, "审批动作不合法");
    String comment = trimToNull(request.getComment());
    if ("reject".equals(action) && comment == null) return Result.error(400, "请填写驳回理由");
    String status = "approve".equals(action) ? "approved" : "rejected";
    Integer reviewerId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    Timestamp reviewedAt = Times.now();
    LeaveApplication update =
        LeaveApplication.builder()
            .id(application.getId())
            .status(status)
            .reviewerId(reviewerId)
            .reviewComment(comment)
            .reviewedAt(reviewedAt)
            .build();
    leaveApplicationMapper.updateById(update);
    application.setStatus(status);
    application.setReviewerId(reviewerId);
    application.setReviewComment(comment);
    application.setReviewedAt(reviewedAt);
    if ("approved".equals(status)) {
      checkInService.syncApprovedLeave(application);
    }
    scheduleBotReviewResultNotification(application);
    return Result.success("approve".equals(action) ? "请假申请已通过" : "请假申请已驳回");
  }

  @Override
  public Result<String> delete(Integer leaveApplicationId) {
    LeaveApplication application = find(leaveApplicationId);
    if (application == null) return Result.error(404, "请假申请不存在");
    if (!canDelete(application)) return Result.error(403, "无权删除该请假申请");
    leaveApplicationMapper.deleteById(leaveApplicationId);
    return Result.success("请假记录已删除");
  }

  private LeaveApplication find(Integer id) {
    return id == null ? null : leaveApplicationMapper.selectById(id);
  }

  private boolean canView(LeaveApplication application) {
    if (!StpUtil.isLogin()) return false;
    Integer currentUserId = StpUtil.getLoginIdAsInt();
    return currentUserId.equals(application.getUserId())
        || StpUtil.hasPermission("leave-application:detail")
        || StpUtil.hasPermission("leave-application:list");
  }

  private boolean canDelete(LeaveApplication application) {
    if (!StpUtil.isLogin()) return false;
    Integer currentUserId = StpUtil.getLoginIdAsInt();
    return currentUserId.equals(application.getUserId()) || StpUtil.hasPermission("leave-application:delete");
  }

  private ResponseLeaveApplicationVO toVO(LeaveApplication application) {
    User applicant = userMapper.selectById(application.getUserId());
    User reviewer = application.getReviewerId() == null ? null : userMapper.selectById(application.getReviewerId());
    List<Map<String, Object>> attachments = normalizedAttachments(application);
    return ResponseLeaveApplicationVO.builder()
        .id(application.getId())
        .userId(application.getUserId())
        .userName(applicant == null ? null : applicant.getUserName())
        .realName(applicant == null ? null : applicant.getRealName())
        .studentId(applicant == null ? null : applicant.getStudentId())
        .title(application.getTitle())
        .reason(application.getReason())
        .startAt(application.getStartAt())
        .endAt(application.getEndAt())
        .attachments(attachments)
        .status(application.getStatus())
        .reviewerId(application.getReviewerId())
        .reviewerName(reviewer == null ? null : reviewer.getRealName())
        .reviewComment(application.getReviewComment())
        .reviewedAt(application.getReviewedAt())
        .createdAt(application.getCreatedAt())
        .approvalFlow(buildFlow(application))
        .build();
  }

  private List<Map<String, Object>> buildFlow(LeaveApplication application) {
    Map<String, Object> submit = new LinkedHashMap<>();
    submit.put("node", "提交申请");
    submit.put("status", "done");
    submit.put("time", application.getCreatedAt());
    submit.put("comment", application.getReason());

    Map<String, Object> review = new LinkedHashMap<>();
    review.put("node", "管理员审批");
    review.put("status", "submitted".equals(application.getStatus()) ? "process" : "done");
    review.put("time", application.getReviewedAt());
    review.put("comment", application.getReviewComment());

    Map<String, Object> finish = new LinkedHashMap<>();
    finish.put("node", statusText(application.getStatus()));
    finish.put("status", "submitted".equals(application.getStatus()) ? "wait" : "done");
    finish.put("time", application.getReviewedAt());
    finish.put("comment", application.getReviewComment());
    return List.of(submit, review, finish);
  }

  private String statusText(String status) {
    return switch (status == null ? "" : status) {
      case "approved" -> "审批通过";
      case "rejected" -> "审批驳回";
      default -> "等待结果";
    };
  }

  private List<Map<String, Object>> sanitizeAttachments(
      List<Map<String, Object>> values, Integer uploaderId) throws IOException {
    if (values == null) return List.of();
    List<Map<String, Object>> attachments = new ArrayList<>();
    for (Map<String, Object> item : values) {
      if (item == null) continue;
      Map<String, Object> attachment = normalizeAttachment(item, uploaderId);
      if (attachment != null) attachments.add(attachment);
      if (attachments.size() >= 5) break;
    }
    return attachments;
  }

  private List<Map<String, Object>> normalizedAttachments(LeaveApplication application) {
    List<Map<String, Object>> attachments = Jsons.parseListOfObjects(application.getAttachments());
    if (attachments.isEmpty()) return attachments;
    try {
      List<Map<String, Object>> normalized = sanitizeAttachments(attachments, application.getUserId());
      String normalizedJson = Jsons.stringify(normalized);
      if (!normalizedJson.equals(application.getAttachments())) {
        LeaveApplication update =
            LeaveApplication.builder()
                .id(application.getId())
                .attachments(normalizedJson)
                .build();
        leaveApplicationMapper.updateById(update);
        application.setAttachments(normalizedJson);
      }
      return normalized;
    } catch (IOException | IllegalArgumentException e) {
      log.warn("Leave attachment normalization failed: leaveId={}, error={}", application.getId(), e.getMessage());
      return compactAttachments(attachments);
    }
  }

  private Map<String, Object> normalizeAttachment(Map<String, Object> item, Integer uploaderId)
      throws IOException {
    String name = stringValue(item.get("name"));
    String type = stringValue(item.get("type"));
    Object size = item.get("size");
    String content = trimToNull(stringValue(item.get("content")));
    String url = trimToNull(stringValue(item.get("url")));
    if (isHttpUrl(url) && (content == null || isDataImage(content))) {
      content = url;
    }
    if (name == null || content == null) return null;
    Map<String, Object> value = new LinkedHashMap<>();
    if (isDataImage(content)) {
      StoredLeaveAttachment attachment = leaveAttachmentStorageService.uploadDataUrl(content, name);
      String attachmentUrl = leaveAttachmentUrl(attachment.getFileName());
      value.put("name", attachment.getOriginalName());
      value.put("type", attachment.getMediaType().toString());
      value.put("size", attachment.getFileSize());
      value.put("content", attachmentUrl);
      value.put("url", attachmentUrl);
      return value;
    }
    value.put("name", name);
    value.put("type", type == null ? "" : type);
    value.put("size", size);
    value.put("content", content);
    if (isHttpUrl(content)) value.put("url", content);
    return value;
  }

  private List<Map<String, Object>> compactAttachments(List<Map<String, Object>> attachments) {
    if (attachments == null) return List.of();
    return attachments.stream()
        .filter(item -> item != null)
        .limit(5)
        .map(item -> {
          Map<String, Object> value = new LinkedHashMap<>();
          value.put("name", stringValue(item.get("name")));
          value.put("type", stringValue(item.getOrDefault("type", "")));
          value.put("size", item.get("size"));
          String content = trimToNull(stringValue(item.get("content")));
          if (isDataImage(content)) {
            value.put("content", "");
            value.put("unavailableReason", "历史内嵌图片暂未生成地址");
          } else {
            value.put("content", content);
            if (isHttpUrl(content)) value.put("url", content);
          }
          return value;
        })
        .toList();
  }

  private Club defaultClub() {
    return clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private boolean isDataImage(String value) {
    return value != null && value.startsWith("data:image/");
  }

  private boolean isHttpUrl(String value) {
    return value != null
        && (value.startsWith("http://")
            || value.startsWith("https://")
            || value.startsWith("/leave-attachments/")
            || value.startsWith("/files/images/"));
  }

  private String leaveAttachmentUrl(String fileName) {
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath()
          .path("/leave-attachments/")
          .path(fileName)
          .toUriString();
    } catch (IllegalStateException e) {
      return "/leave-attachments/" + fileName;
    }
  }

  private void scheduleBotReviewResultNotification(LeaveApplication application) {
    if (trimToNull(application.getBotNotifyOrigin()) == null) return;
    LeaveApplication snapshot =
        LeaveApplication.builder()
            .id(application.getId())
            .status(application.getStatus())
            .title(application.getTitle())
            .reviewComment(application.getReviewComment())
            .botNotifyOrigin(application.getBotNotifyOrigin())
            .botNotifyUserId(application.getBotNotifyUserId())
            .build();
    CompletableFuture.runAsync(() -> notifyBotReviewResult(snapshot))
        .exceptionally(
            throwable -> {
              log.warn(
                  "Bot leave review callback scheduling failed: leaveId={}, error={}",
                  snapshot.getId(),
                  throwable.getMessage());
              return null;
            });
  }

  private void notifyBotReviewResult(LeaveApplication application) {
    String callbackUrl = trimToNull(leaveReviewCallbackUrl);
    if (callbackUrl == null) {
      log.warn("Bot leave review callback skipped: callback url is empty, leaveId={}", application.getId());
      return;
    }
    if (trimToNull(application.getBotNotifyOrigin()) == null) {
      log.warn("Bot leave review callback skipped: missing notify origin, leaveId={}", application.getId());
      return;
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("leaveId", application.getId());
    payload.put("status", application.getStatus());
    payload.put("title", application.getTitle());
    payload.put("reviewComment", application.getReviewComment());
    payload.put("botNotifyOrigin", application.getBotNotifyOrigin());
    payload.put("botNotifyUserId", application.getBotNotifyUserId());
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(callbackUrl))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(Jsons.stringify(payload)));
    String token = trimToNull(botCallbackToken);
    if (token != null) {
      builder.header("X-OpenAtom-Bot-Token", token);
    }
    httpClient
        .sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding())
        .thenAccept(
            response -> {
              if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LeaveApplication update =
                    LeaveApplication.builder()
                        .id(application.getId())
                        .botNotifiedAt(Times.now())
                        .build();
                leaveApplicationMapper.updateById(update);
              } else {
                log.warn("Bot leave review callback failed: leaveId={}, status={}", application.getId(), response.statusCode());
              }
            })
        .exceptionally(
            throwable -> {
              log.warn("Bot leave review callback failed: leaveId={}, error={}", application.getId(), throwable.getMessage());
              return null;
            });
  }
}
