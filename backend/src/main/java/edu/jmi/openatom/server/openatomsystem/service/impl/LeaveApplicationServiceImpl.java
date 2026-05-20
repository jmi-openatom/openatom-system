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
import edu.jmi.openatom.server.openatomsystem.service.LeaveApplicationService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLeaveApplicationVO;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveApplicationServiceImpl implements LeaveApplicationService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";

  private final ClubMapper clubMapper;
  private final UserMapper userMapper;
  private final LeaveApplicationMapper leaveApplicationMapper;

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
        request.getStartAt(), request.getEndAt(), request.getAttachments());
  }

  private Result<Integer> createForUser(
      Integer userId,
      String title,
      String reason,
      String startAt,
      String endAt,
      List<Map<String, Object>> rawAttachments) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    List<Map<String, Object>> attachments = sanitizeAttachments(rawAttachments);
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
    application.setStatus("approve".equals(action) ? "approved" : "rejected");
    application.setReviewerId(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null);
    application.setReviewComment(comment);
    application.setReviewedAt(Times.now());
    leaveApplicationMapper.updateById(application);
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
    List<Map<String, Object>> attachments = Jsons.parseListOfObjects(application.getAttachments());
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

  private List<Map<String, Object>> sanitizeAttachments(List<Map<String, Object>> values) {
    if (values == null) return List.of();
    return values.stream()
        .filter(item -> item != null && item.get("name") != null && item.get("content") != null)
        .limit(5)
        .map(item -> {
          Map<String, Object> value = new LinkedHashMap<>();
          value.put("name", String.valueOf(item.get("name")));
          value.put("type", String.valueOf(item.getOrDefault("type", "")));
          value.put("size", item.get("size"));
          value.put("content", String.valueOf(item.get("content")));
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
}
