package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.InterviewQueueService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewQueueVO;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewQueueServiceImpl implements InterviewQueueService {
  private final InterviewSessionMapper sessionMapper;
  private final InterviewRoomMapper roomMapper;
  private final InterviewMapper interviewMapper;
  private final InterviewQueueStateMapper queueMapper;
  private final InterviewQueueOperationMapper operationMapper;
  private final InterviewRoomInterviewerMapper roomInterviewerMapper;
  private final InterviewInterviewerMapper interviewerMapper;
  private final InterviewFeedbackMapper feedbackMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final UserMapper userMapper;

  @Override
  public Result<ResponseInterviewQueueVO> detail(Integer sessionId) {
    InterviewSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    List<Interview> interviews = interviewMapper.selectBySessionId(sessionId).stream()
        .filter(i -> !"draft".equals(i.getStatus())).toList();
    Map<Integer, InterviewQueueState> states = queueMapper.selectBySessionId(sessionId).stream()
        .collect(Collectors.toMap(InterviewQueueState::getInterviewId, Function.identity(), (a, b) -> a));
    Map<Integer, Interview> interviewMap = interviews.stream()
        .collect(Collectors.toMap(Interview::getId, Function.identity()));
    Map<Integer, String> roomNames = roomMapper.selectBySessionId(sessionId).stream()
        .collect(Collectors.toMap(InterviewRoom::getId, InterviewRoom::getName));
    List<ResponseInterviewQueueVO.Candidate> candidates = interviews.stream()
        .map(i -> candidate(i, states.get(i.getId()), roomNames.get(i.getRoomId()))).toList();
    Map<Integer, ResponseInterviewQueueVO.Candidate> candidateMap = candidates.stream()
        .collect(Collectors.toMap(ResponseInterviewQueueVO.Candidate::getInterviewId, Function.identity()));
    List<ResponseInterviewQueueVO.Room> rooms = roomMapper.selectBySessionId(sessionId).stream().map(room -> {
      List<InterviewQueueState> roomStates = states.values().stream()
          .filter(s -> Objects.equals(s.getRoomId(), room.getId())).toList();
      ResponseInterviewQueueVO.Candidate current = roomStates.stream()
          .filter(s -> "called".equals(s.getStatus()))
          .max(Comparator.comparing(InterviewQueueState::getCalledAt,
              Comparator.nullsLast(Comparator.naturalOrder())))
          .map(s -> candidateMap.get(s.getInterviewId())).orElse(null);
      int waiting = (int) roomStates.stream().filter(s -> "waiting".equals(s.getStatus())
          && interviewMap.containsKey(s.getInterviewId())).count();
      return ResponseInterviewQueueVO.Room.builder().roomId(room.getId()).name(room.getName())
          .location(room.getLocation()).waitingCount(waiting).current(current).build();
    }).toList();
    ResponseInterviewQueueVO.Stats stats = ResponseInterviewQueueVO.Stats.builder()
        .total(candidates.size())
        .checkedIn((int) candidates.stream().filter(c -> List.of("waiting", "called", "completed").contains(c.getQueueStatus())).count())
        .waiting((int) candidates.stream().filter(c -> "waiting".equals(c.getQueueStatus())).count())
        .called((int) candidates.stream().filter(c -> "called".equals(c.getQueueStatus())).count())
        .completed((int) candidates.stream().filter(c -> "completed".equals(c.getQueueStatus())).count())
        .noShow((int) candidates.stream().filter(c -> "no_show".equals(c.getQueueStatus())).count())
        .notCheckedIn((int) candidates.stream().filter(c -> List.of("not_checked_in", "cancelled").contains(c.getQueueStatus())).count())
        .build();
    return Result.success(ResponseInterviewQueueVO.builder().sessionId(sessionId)
        .sessionName(session.getName()).sessionStatus(session.getStatus())
        .serverTime(new Timestamp(System.currentTimeMillis())).stats(stats).rooms(rooms).candidates(candidates).build());
  }

  @Override
  public Result<ResponseInterviewQueueVO> callScreen(Integer sessionId) {
    Result<ResponseInterviewQueueVO> result = detail(sessionId);
    if (result.getData() != null) result.getData().setCandidates(List.of());
    return result;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> checkIn(Integer interviewId) {
    Interview interview = findPublishedInterview(interviewId);
    if (interview == null) return Result.error(404, "找不到可签到的已发布面试安排");
    return checkInInterview(interview, StpUtil.getLoginIdAsInt(), Map.of());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> selfCheckIn(Integer sessionId, String studentId) {
    if (!isPublished(sessionId)) return Result.error(422, "本场次暂未开放签到");
    String normalizedStudentId = text(studentId);
    if (normalizedStudentId == null || normalizedStudentId.isBlank()) return Result.error(400, "请输入学号");
    Interview interview = interviewMapper.selectBySessionId(sessionId).stream()
        .filter(item -> !"draft".equals(item.getStatus()))
        .filter(item -> matchesStudentId(item, normalizedStudentId))
        .findFirst().orElse(null);
    if (interview == null) return Result.error(404, "未找到本场次可签到的候选人，请核对学号后重试");
    return checkInInterview(interview, null, Map.of("source", "self_service"));
  }

  private Result<String> checkInInterview(Interview interview, Integer checkedInBy,
      Map<String, Object> detail) {
    if ("completed".equals(interview.getStatus())) return Result.error(422, "该候选人的面试已完成");
    InterviewQueueState state = queueMapper.selectByInterviewId(interview.getId());
    if (state != null && !"cancelled".equals(state.getStatus())) return Result.error(422, "候选人已经签到");
    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (state == null) {
      state = InterviewQueueState.builder().interviewId(interview.getId()).sessionId(interview.getSessionId())
          .roomId(interview.getRoomId()).status("waiting").checkedInBy(checkedInBy)
          .checkedInAt(now).callCount(0).build();
      queueMapper.insert(state);
    } else {
      state.setStatus("waiting"); state.setCheckedInBy(checkedInBy);
      state.setCheckedInAt(now); state.setCalledAt(null); state.setCallCount(0); queueMapper.updateById(state);
    }
    log(interview.getSessionId(), interview.getId(), interview.getRoomId(), "check_in", detail);
    return Result.success("签到成功，已进入候场队列");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> undoCheckIn(Integer interviewId) {
    InterviewQueueState state = queueMapper.selectByInterviewId(interviewId);
    if (state == null) return Result.error(404, "候选人尚未签到");
    if (!"waiting".equals(state.getStatus())) return Result.error(422, "候选人已叫号，不能撤销签到");
    state.setStatus("cancelled"); state.setCheckedInAt(null); state.setCalledAt(null);
    queueMapper.updateById(state);
    log(state.getSessionId(), interviewId, state.getRoomId(), "undo_check_in", Map.of());
    return Result.success("已撤销签到");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseInterviewQueueVO.Candidate> callNext(Integer roomId) {
    InterviewRoom room = roomId == null ? null : roomMapper.selectByIdForUpdate(roomId);
    if (room == null) return Result.error(404, "面试间不存在");
    if (!isPublished(room.getSessionId())) return Result.error(422, "面试场次未发布或已经结束");
    List<InterviewQueueState> active = queueMapper.selectActiveByRoomId(roomId);
    for (InterviewQueueState current : active) {
      Interview currentInterview = interviewMapper.selectById(current.getInterviewId());
      if (currentInterview != null && !"completed".equals(currentInterview.getStatus())) {
        return Result.error(422, "上一位候选人的所有面试官尚未提交评价，暂不能叫下一位");
      }
      current.setStatus("completed"); queueMapper.updateById(current);
    }
    List<Interview> roomInterviews = interviewMapper.selectBySessionId(room.getSessionId()).stream()
        .filter(i -> Objects.equals(i.getRoomId(), roomId)).toList();
    Map<Integer, Interview> interviewMap = roomInterviews.stream()
        .collect(Collectors.toMap(Interview::getId, Function.identity()));
    List<InterviewQueueState> roomQueueStates = queueMapper.selectBySessionId(room.getSessionId()).stream()
        .filter(s -> Objects.equals(s.getRoomId(), roomId))
        .filter(s -> interviewMap.containsKey(s.getInterviewId()))
        .toList();
    for (InterviewQueueState state : roomQueueStates) {
      Interview queuedInterview = interviewMap.get(state.getInterviewId());
      if ("waiting".equals(state.getStatus()) && "completed".equals(queuedInterview.getStatus())) {
        state.setStatus("completed");
        queueMapper.updateById(state);
      }
    }
    InterviewQueueState next = roomQueueStates.stream()
        .filter(s -> Objects.equals(s.getRoomId(), roomId) && "waiting".equals(s.getStatus()))
        .filter(s -> !"completed".equals(interviewMap.get(s.getInterviewId()).getStatus()))
        .min(Comparator.comparing((InterviewQueueState s) -> interviewMap.get(s.getInterviewId()).getQueueNumber(),
            Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(InterviewQueueState::getCheckedInAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(null);
    if (next == null) return Result.error(422, "当前面试间没有已签到的候选人");
    next.setStatus("called"); next.setCalledAt(new Timestamp(System.currentTimeMillis()));
    next.setCallCount(1); queueMapper.updateById(next);
    log(next.getSessionId(), next.getInterviewId(), roomId, "call_next", Map.of("callCount", 1));
    return Result.success(candidate(interviewMap.get(next.getInterviewId()), next, room.getName()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseInterviewQueueVO.Candidate> callAgain(Integer roomId) {
    InterviewRoom room = roomId == null ? null : roomMapper.selectByIdForUpdate(roomId);
    if (room == null) return Result.error(404, "面试间不存在");
    if (!isPublished(room.getSessionId())) return Result.error(422, "面试场次未发布或已经结束");
    InterviewQueueState current = queueMapper.selectActiveByRoomId(roomId).stream().findFirst().orElse(null);
    if (current == null) return Result.error(422, "当前面试间没有正在叫号的候选人");
    current.setCallCount((current.getCallCount() == null ? 0 : current.getCallCount()) + 1);
    current.setCalledAt(new Timestamp(System.currentTimeMillis())); queueMapper.updateById(current);
    log(current.getSessionId(), current.getInterviewId(), roomId, "call_again",
        Map.of("callCount", current.getCallCount()));
    return Result.success(candidate(interviewMapper.selectById(current.getInterviewId()), current, room.getName()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> markNoShow(Integer interviewId) {
    Interview interview = findPublishedInterview(interviewId);
    if (interview == null) return Result.error(404, "找不到可处理的面试安排");
    InterviewQueueState state = queueMapper.selectByInterviewId(interviewId);
    if (state != null && List.of("called", "completed").contains(state.getStatus())) {
      return Result.error(422, "候选人已叫号或已完成，不能标记缺席");
    }
    if (state == null) {
      state = InterviewQueueState.builder().interviewId(interviewId).sessionId(interview.getSessionId())
          .roomId(interview.getRoomId()).status("no_show").callCount(0).build();
      queueMapper.insert(state);
    } else {
      state.setStatus("no_show"); state.setCalledAt(null); queueMapper.updateById(state);
    }
    log(interview.getSessionId(), interviewId, interview.getRoomId(), "mark_no_show", Map.of());
    return Result.success("已标记为缺席/过号，可稍后恢复候场");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> restoreWaiting(Integer interviewId) {
    Interview interview = findPublishedInterview(interviewId);
    if (interview == null) return Result.error(404, "找不到可恢复的面试安排");
    InterviewQueueState state = queueMapper.selectByInterviewId(interviewId);
    if (state == null || !List.of("no_show", "cancelled").contains(state.getStatus())) {
      return Result.error(422, "当前状态不需要恢复");
    }
    state.setStatus("waiting"); state.setCheckedInBy(StpUtil.getLoginIdAsInt());
    state.setCheckedInAt(new Timestamp(System.currentTimeMillis())); state.setCalledAt(null);
    queueMapper.updateById(state);
    log(state.getSessionId(), interviewId, state.getRoomId(), "restore_waiting", Map.of());
    return Result.success("已恢复到候场队列末尾");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> moveRoom(Integer interviewId, Integer targetRoomId) {
    Interview interview = findPublishedInterview(interviewId);
    if (interview == null) return Result.error(404, "找不到可调整的面试安排");
    InterviewRoom target = targetRoomId == null ? null : roomMapper.selectByIdForUpdate(targetRoomId);
    if (target == null || !Objects.equals(target.getSessionId(), interview.getSessionId())) {
      return Result.error(422, "目标面试间不属于当前场次");
    }
    InterviewQueueState state = queueMapper.selectByInterviewId(interviewId);
    if (state != null && List.of("called", "completed").contains(state.getStatus())) {
      return Result.error(422, "候选人已叫号或已完成，不能临时换房");
    }
    Integer previousRoomId = interview.getRoomId();
    if (Objects.equals(previousRoomId, targetRoomId)) return Result.error(422, "候选人已在该面试间");
    interview.setRoomId(targetRoomId); interview.setLocation(target.getLocation()); interviewMapper.updateById(interview);
    if (state != null) { state.setRoomId(targetRoomId); queueMapper.updateById(state); }
    interviewerMapper.deleteByInterviewId(interviewId);
    roomInterviewerMapper.selectByRoomId(targetRoomId).stream()
        .map(InterviewRoomInterviewer::getInterviewerId).distinct()
        .forEach(id -> interviewerMapper.insert(InterviewInterviewer.builder()
            .interviewId(interviewId).interviewerId(id).build()));
    log(interview.getSessionId(), interviewId, targetRoomId, "move_room",
        Map.of("fromRoomId", previousRoomId, "toRoomId", targetRoomId));
    return Result.success("已调整面试间并同步目标房间面试官组");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> recoverRoom(Integer roomId) {
    InterviewRoom room = roomId == null ? null : roomMapper.selectByIdForUpdate(roomId);
    if (room == null) return Result.error(404, "面试间不存在");
    if (!isPublished(room.getSessionId())) return Result.error(422, "面试场次未发布或已经结束");
    InterviewQueueState current = queueMapper.selectActiveByRoomId(roomId).stream().findFirst().orElse(null);
    if (current == null) return Result.error(422, "当前面试间没有需要恢复的叫号状态");
    Interview interview = interviewMapper.selectById(current.getInterviewId());
    if (interview != null && "completed".equals(interview.getStatus())) current.setStatus("completed");
    else { current.setStatus("waiting"); current.setCalledAt(null); }
    queueMapper.updateById(current);
    log(room.getSessionId(), current.getInterviewId(), roomId, "recover_room",
        Map.of("resultStatus", current.getStatus()));
    return Result.success("面试间状态已恢复");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> completeSession(Integer sessionId) {
    InterviewSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    if ("completed".equals(session.getStatus())) return Result.error(422, "场次已经结束");
    List<Interview> interviews = interviewMapper.selectBySessionId(sessionId).stream()
        .filter(i -> !"draft".equals(i.getStatus())).toList();
    Map<Integer, InterviewQueueState> states = queueMapper.selectBySessionId(sessionId).stream()
        .collect(Collectors.toMap(InterviewQueueState::getInterviewId, Function.identity(), (a, b) -> a));
    for (Interview interview : interviews) {
      InterviewQueueState state = states.get(interview.getId());
      if (state == null || !List.of("completed", "no_show", "cancelled").contains(state.getStatus())) {
        return Result.error(422, "仍有候选人未处理，请完成面试或标记缺席后再结束场次");
      }
    }
    session.setStatus("completed"); sessionMapper.updateById(session);
    log(sessionId, null, null, "complete_session", Map.of("candidateCount", interviews.size()));
    return Result.success("面试场次已结束，现场队列已锁定");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> reopenSession(Integer sessionId) {
    InterviewSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    if (!"completed".equals(session.getStatus())) return Result.error(422, "只有已结束场次可以重新开启");
    session.setStatus("published"); sessionMapper.updateById(session);
    log(sessionId, null, null, "reopen_session", Map.of());
    return Result.success("场次已重新开启");
  }

  @Override
  public Result<List<InterviewQueueOperation>> operations(Integer sessionId) {
    if (sessionMapper.selectById(sessionId) == null) return Result.error(404, "面试场次不存在");
    return Result.success(operationMapper.selectRecentBySessionId(sessionId));
  }

  @Override
  public Result<byte[]> exportEvaluationSummary(Integer sessionId) {
    InterviewSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    Map<Integer, String> roomNames = roomMapper.selectBySessionId(sessionId).stream()
        .collect(Collectors.toMap(InterviewRoom::getId, InterviewRoom::getName));
    Map<Integer, InterviewQueueState> states = queueMapper.selectBySessionId(sessionId).stream()
        .collect(Collectors.toMap(InterviewQueueState::getInterviewId, Function.identity(), (a, b) -> a));
    StringBuilder csv = new StringBuilder("\uFEFF编号,姓名,学号,面试间,现场状态,面试状态,评价进度,平均分,录用建议,评价汇总\r\n");
    for (Interview interview : interviewMapper.selectBySessionId(sessionId)) {
      ResponseInterviewQueueVO.Candidate candidate = candidate(interview, states.get(interview.getId()), roomNames.get(interview.getRoomId()));
      Map<Integer, InterviewFeedback> latest = new LinkedHashMap<>();
      feedbackMapper.selectByInterviewId(interview.getId()).forEach(f -> latest.putIfAbsent(f.getInterviewerId(), f));
      List<InterviewFeedback> submitted = latest.values().stream().filter(f -> "submitted".equals(f.getStatus())).toList();
      int required = interviewerMapper.selectByInterviewId(interview.getId()).stream()
          .map(InterviewInterviewer::getInterviewerId).distinct().toList().size();
      List<Double> values = submitted.stream().flatMap(f -> Jsons.parseObject(f.getScores()).values().stream())
          .filter(Number.class::isInstance).map(v -> ((Number) v).doubleValue()).toList();
      String average = values.isEmpty() ? "" : String.format(Locale.ROOT, "%.2f", values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
      String suggestions = submitted.stream().map(InterviewFeedback::getSuggestion).filter(Objects::nonNull).collect(Collectors.joining(" / "));
      String comments = submitted.stream().map(f -> "面试官#" + f.getInterviewerId() + "：" + Objects.toString(f.getComment(), ""))
          .collect(Collectors.joining(" | "));
      csv.append(csv(candidate.getQueueNumber())).append(',').append(csv(candidate.getApplicantName())).append(',')
          .append(csv(candidate.getStudentId())).append(',').append(csv(candidate.getRoomName())).append(',')
          .append(csv(candidate.getQueueStatus())).append(',').append(csv(candidate.getInterviewStatus())).append(',')
          .append(csv(submitted.size() + "/" + required)).append(',').append(csv(average)).append(',')
          .append(csv(suggestions)).append(',').append(csv(comments)).append("\r\n");
    }
    return Result.success(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private Interview findPublishedInterview(Integer interviewId) {
    Interview interview = interviewId == null ? null : interviewMapper.selectById(interviewId);
    if (interview == null || interview.getSessionId() == null || interview.getRoomId() == null) return null;
    InterviewSession session = sessionMapper.selectById(interview.getSessionId());
    return session != null && "published".equals(session.getStatus()) ? interview : null;
  }

  private boolean isPublished(Integer sessionId) {
    InterviewSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
    return session != null && "published".equals(session.getStatus());
  }

  private boolean matchesStudentId(Interview interview, String studentId) {
    MembershipApplication application = applicationMapper.selectById(interview.getApplicationId());
    if (application == null) return false;
    User user = application.getUserId() == null ? null : userMapper.selectById(application.getUserId());
    String applicationStudentId = text(Jsons.parseObject(application.getProfile()).get("studentId"));
    return studentId.equals(user == null ? applicationStudentId : user.getStudentId())
        || studentId.equals(applicationStudentId);
  }

  private ResponseInterviewQueueVO.Candidate candidate(
      Interview interview, InterviewQueueState state, String roomName) {
    MembershipApplication application = interview == null ? null : applicationMapper.selectById(interview.getApplicationId());
    User user = application == null || application.getUserId() == null ? null : userMapper.selectById(application.getUserId());
    Map<String, Object> profile = application == null ? Map.of() : Jsons.parseObject(application.getProfile());
    String name = user != null && user.getRealName() != null ? user.getRealName()
        : firstNonBlank(text(profile.get("applicantName")), text(profile.get("name")),
            text(profile.get("realName")), "姓名未填写");
    String studentId = user == null ? text(profile.get("studentId")) : user.getStudentId();
    return ResponseInterviewQueueVO.Candidate.builder().interviewId(interview.getId())
        .applicationId(interview.getApplicationId()).roomId(interview.getRoomId()).roomName(roomName)
        .queueNumber(interview.getQueueNumber()).applicantName(name).studentId(studentId)
        .scheduledStartAt(interview.getScheduledStartAt()).interviewStatus(interview.getStatus())
        .queueStatus(state == null ? "not_checked_in" : state.getStatus())
        .checkedInAt(state == null ? null : state.getCheckedInAt())
        .calledAt(state == null ? null : state.getCalledAt())
        .callCount(state == null || state.getCallCount() == null ? 0 : state.getCallCount()).build();
  }

  private void log(Integer sessionId, Integer interviewId, Integer roomId,
      String action, Map<String, Object> detail) {
    Integer operatorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    operationMapper.insert(InterviewQueueOperation.builder().sessionId(sessionId)
        .interviewId(interviewId).roomId(roomId).action(action).operatorId(operatorId)
        .detailJson(Jsons.stringify(detail)).build());
  }

  private String csv(Object value) {
    String text = value == null ? "" : String.valueOf(value);
    return "\"" + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
  }

  private String text(Object value) { return value == null ? null : String.valueOf(value); }
  private String firstNonBlank(String... values) {
    for (String value : values) if (value != null && !value.isBlank()) return value.trim();
    return null;
  }
}
