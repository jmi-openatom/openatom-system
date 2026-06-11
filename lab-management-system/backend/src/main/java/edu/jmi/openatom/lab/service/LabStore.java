package edu.jmi.openatom.lab.service;

import edu.jmi.openatom.lab.domain.LabModels.CmsLoginRequest;
import edu.jmi.openatom.lab.domain.LabModels.Dashboard;
import edu.jmi.openatom.lab.domain.LabModels.LabCheckinLog;
import edu.jmi.openatom.lab.domain.LabModels.LabNotice;
import edu.jmi.openatom.lab.domain.LabModels.LabProblem;
import edu.jmi.openatom.lab.domain.LabModels.LabProblemCase;
import edu.jmi.openatom.lab.domain.LabModels.LabSettings;
import edu.jmi.openatom.lab.domain.LabModels.LabSubmission;
import edu.jmi.openatom.lab.domain.LabModels.LabUser;
import edu.jmi.openatom.lab.domain.LabModels.LabUserRequest;
import edu.jmi.openatom.lab.domain.LabModels.NoticeRequest;
import edu.jmi.openatom.lab.domain.LabModels.ProblemRequest;
import edu.jmi.openatom.lab.domain.LabModels.SessionResponse;
import edu.jmi.openatom.lab.domain.LabModels.SettingsRequest;
import edu.jmi.openatom.lab.domain.LabModels.SubmissionRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LabStore {
  private static final int ATTENDANCE_ABSENT = 0;
  private static final int ATTENDANCE_OJ_AC = 1;
  private static final int ATTENDANCE_ONSITE = 2;
  private static final int ATTENDANCE_LATE = 3;

  private final AtomicLong ids = new AtomicLong(1);
  private final Map<Long, LabUser> users = new ConcurrentHashMap<>();
  private final Map<Long, LabProblem> problems = new ConcurrentHashMap<>();
  private final Map<Long, LabSubmission> submissions = new ConcurrentHashMap<>();
  private final Map<Long, LabCheckinLog> checkins = new ConcurrentHashMap<>();
  private final Map<Long, LabNotice> notices = new ConcurrentHashMap<>();
  private final Map<String, Long> sessions = new ConcurrentHashMap<>();
  private LabSettings settings = new LabSettings(LocalTime.of(9, 0), 30, 2, -5, -10);

  public LabStore() {
    LabUser coach = new LabUser(nextId(), 10001L, "coach", "实验室主教练", 2, 100, true, Instant.now());
    users.put(coach.id(), coach);
    LabProblem problem = defaultProblem();
    problems.put(problem.id(), problem);
    notices.put(
        nextId(),
        new LabNotice(
            nextId(),
            null,
            "今日算法挑战已更新",
            "请在 23:59 前完成每日一练 AC，系统会自动记录实验室签到。",
            "problem",
            false,
            Instant.now()));
  }

  public String cmsAuthorizeUrl(String redirectUri) {
    return "http://localhost:8080/api/v1/oauth/authorize?client_id=lab-management-system&response_type=code&redirect_uri="
        + redirectUri;
  }

  public SessionResponse loginFromCms(CmsLoginRequest request) {
    if (!Boolean.TRUE.equals(request.isLabMember())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "您当前非实验室正式成员，请联系管理员在社团管理系统开通权限。");
    }
    LabUser user =
        users.values().stream()
            .filter(item -> item.clubUserId().equals(request.clubUserId()))
            .findFirst()
            .orElseGet(
                () -> {
                  LabUser created =
                      new LabUser(
                          nextId(),
                          request.clubUserId(),
                          request.username(),
                          request.realName(),
                          0,
                          100,
                          true,
                          Instant.now());
                  users.put(created.id(), created);
                  return created;
                });
    if (!user.active()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "您当前非实验室正式成员，请联系管理员在社团管理系统开通权限。");
    }
    String token = UUID.randomUUID().toString();
    sessions.put(token, user.id());
    return new SessionResponse(token, user);
  }

  public LabUser currentUser(String authorization) {
    return userByToken(authorization);
  }

  public LabProblem todayProblem() {
    LocalDate today = LocalDate.now();
    return problems.values().stream()
        .filter(problem -> "published".equals(problem.status()))
        .filter(problem -> today.equals(problem.publishDate()))
        .findFirst()
        .orElseGet(
            () ->
                problems.values().stream()
                    .filter(problem -> "published".equals(problem.status()))
                    .max(Comparator.comparing(LabProblem::publishDate))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无每日一练")));
  }

  public List<LabProblem> publicProblems() {
    return problems.values().stream()
        .filter(problem -> "published".equals(problem.status()))
        .sorted(Comparator.comparing(LabProblem::publishDate).reversed())
        .toList();
  }

  public LabSubmission submit(String authorization, Long problemId, SubmissionRequest request) {
    LabUser user = userByToken(authorization);
    LabProblem problem = problem(problemId);
    JudgeResult result = judge(problem, request);
    LabSubmission submission =
        new LabSubmission(
            nextId(),
            problem.id(),
            user.id(),
            normalizeLanguage(request.language()),
            result.status(),
            result.message(),
            result.passedCases(),
            problem.cases().size(),
            Instant.now());
    submissions.put(submission.id(), submission);
    if ("AC".equals(submission.status())) {
      applyAcceptedCheckin(user, problem, submission);
    }
    return submission;
  }

  public List<LabSubmission> submissions(String authorization) {
    LabUser user = userByToken(authorization);
    return submissions.values().stream()
        .filter(item -> item.labUserId().equals(user.id()))
        .sorted(Comparator.comparing(LabSubmission::createdAt).reversed())
        .toList();
  }

  public LabCheckinLog todayCheckin(String authorization) {
    LabUser user = userByToken(authorization);
    return checkins.values().stream()
        .filter(item -> item.labUserId().equals(user.id()) && item.checkinDate().equals(LocalDate.now()))
        .findFirst()
        .orElse(null);
  }

  public LabCheckinLog onsiteCheckin(String authorization) {
    LabUser user = userByToken(authorization);
    LabCheckinLog existing = todayCheckin(authorization);
    if (existing != null) return existing;
    boolean late = LocalTime.now().isAfter(settings.standardCheckinTime().plusMinutes(settings.lateMinutes()));
    LabCheckinLog log =
        createCheckin(
            user,
            late ? ATTENDANCE_LATE : ATTENDANCE_ONSITE,
            "onsite",
            null,
            late ? settings.latePenalty() : 0,
            late ? 0 : settings.normalClubPoints(),
            late ? "现场签到迟到，已扣减实验室信誉分" : "现场签到成功，社团积分待同步");
    if (late) updateReputation(user, settings.latePenalty());
    return log;
  }

  public List<LabCheckinLog> scoreLogs(String authorization) {
    LabUser user = userByToken(authorization);
    return checkins.values().stream()
        .filter(item -> item.labUserId().equals(user.id()))
        .sorted(Comparator.comparing(LabCheckinLog::createdAt).reversed())
        .toList();
  }

  public List<LabNotice> notices(String authorization) {
    LabUser user = userByToken(authorization);
    return notices.values().stream()
        .filter(item -> item.receiverLabUserId() == null || item.receiverLabUserId().equals(user.id()))
        .sorted(Comparator.comparing(LabNotice::createdAt).reversed())
        .toList();
  }

  public Dashboard dashboard(String authorization) {
    requireAdmin(authorization);
    LocalDate today = LocalDate.now();
    long todaySubmissionCount =
        submissions.values().stream().filter(item -> item.createdAt().isAfter(today.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant())).count();
    long todayAcceptedCount =
        submissions.values().stream()
            .filter(item -> "AC".equals(item.status()))
            .filter(item -> item.createdAt().isAfter(today.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant()))
            .count();
    long todayCheckedInCount = checkins.values().stream().filter(item -> today.equals(item.checkinDate())).count();
    long pendingSync = checkins.values().stream().filter(item -> item.clubSyncStatus() == 1).count();
    return new Dashboard(
        users.values().stream().filter(LabUser::active).count(),
        problems.size(),
        todaySubmissionCount,
        todayAcceptedCount,
        todayCheckedInCount,
        pendingSync,
        today);
  }

  public List<LabUser> adminUsers(String authorization) {
    requireAdmin(authorization);
    return users.values().stream().sorted(Comparator.comparing(LabUser::id)).toList();
  }

  public LabUser saveUser(String authorization, LabUserRequest request) {
    requireAdmin(authorization);
    LabUser existing =
        users.values().stream()
            .filter(item -> item.clubUserId().equals(request.clubUserId()))
            .findFirst()
            .orElse(null);
    LabUser saved =
        new LabUser(
            existing == null ? nextId() : existing.id(),
            request.clubUserId(),
            request.username(),
            request.realName(),
            request.labRole() == null ? 0 : Math.max(0, Math.min(2, request.labRole())),
            existing == null ? 100 : existing.reputationScore(),
            request.active() == null || request.active(),
            existing == null ? Instant.now() : existing.createdAt());
    users.put(saved.id(), saved);
    return saved;
  }

  public List<LabProblem> adminProblems(String authorization) {
    requireAdmin(authorization);
    return problems.values().stream().sorted(Comparator.comparing(LabProblem::id).reversed()).toList();
  }

  public LabProblem saveProblem(String authorization, ProblemRequest request) {
    requireAdmin(authorization);
    long id = nextId();
    List<LabProblemCase> cases =
        request.cases() == null || request.cases().isEmpty()
            ? defaultCases(id)
            : request.cases().stream()
                .map(item -> new LabProblemCase(nextId(), id, item.inputData(), item.expectedOutput(), item.sampleFlag(), item.sortOrder()))
                .toList();
    LabProblem problem =
        new LabProblem(
            id,
            request.title(),
            request.descriptionMd(),
            blankDefault(request.difficulty(), "easy"),
            request.timeLimitMs() == null ? 1000 : request.timeLimitMs(),
            request.memoryLimitMb() == null ? 128 : request.memoryLimitMb(),
            blankDefault(request.status(), "draft"),
            request.publishDate() == null ? LocalDate.now() : request.publishDate(),
            cases,
            request.standardSolutions() == null ? Map.of() : request.standardSolutions(),
            Instant.now());
    problems.put(problem.id(), problem);
    if ("published".equals(problem.status())) {
      broadcast("今日算法挑战已更新", "新的实验室每日一练已发布，请及时完成 AC 签到。", "problem");
    }
    return problem;
  }

  public LabProblem generateProblem(String authorization) {
    requireAdmin(authorization);
    LabProblem problem = defaultProblem();
    problems.put(problem.id(), problem);
    broadcast("今日算法挑战已更新", "今日算法挑战已更新，请及时前往 AC 并完成签到。", "problem");
    return problem;
  }

  public List<LabSubmission> adminSubmissions(String authorization) {
    requireAdmin(authorization);
    return submissions.values().stream().sorted(Comparator.comparing(LabSubmission::createdAt).reversed()).toList();
  }

  public List<LabCheckinLog> adminCheckins(String authorization) {
    requireAdmin(authorization);
    return checkins.values().stream().sorted(Comparator.comparing(LabCheckinLog::createdAt).reversed()).toList();
  }

  public int closeDay(String authorization) {
    requireAdmin(authorization);
    int created = 0;
    for (LabUser user : users.values()) {
      if (!user.active()) continue;
      boolean hasLog =
          checkins.values().stream()
              .anyMatch(item -> item.labUserId().equals(user.id()) && item.checkinDate().equals(LocalDate.now()));
      if (hasLog) continue;
      createCheckin(user, ATTENDANCE_ABSENT, "close_day", null, settings.absentPenalty(), 0, "未签到，系统结算旷课");
      updateReputation(user, settings.absentPenalty());
      created++;
    }
    return created;
  }

  public LabSettings settings(String authorization) {
    requireAdmin(authorization);
    return settings;
  }

  public LabSettings updateSettings(String authorization, SettingsRequest request) {
    requireAdmin(authorization);
    settings =
        new LabSettings(
            request.standardCheckinTime() == null ? settings.standardCheckinTime() : request.standardCheckinTime(),
            request.lateMinutes() == null ? settings.lateMinutes() : Math.max(0, request.lateMinutes()),
            request.normalClubPoints() == null ? settings.normalClubPoints() : Math.max(0, request.normalClubPoints()),
            request.latePenalty() == null ? settings.latePenalty() : Math.min(0, request.latePenalty()),
            request.absentPenalty() == null ? settings.absentPenalty() : Math.min(0, request.absentPenalty()));
    return settings;
  }

  public List<LabNotice> adminNotices(String authorization) {
    requireAdmin(authorization);
    return notices.values().stream().sorted(Comparator.comparing(LabNotice::createdAt).reversed()).toList();
  }

  public LabNotice createNotice(String authorization, NoticeRequest request) {
    requireAdmin(authorization);
    LabNotice notice =
        new LabNotice(nextId(), request.receiverLabUserId(), request.title(), request.content(), blankDefault(request.type(), "system"), false, Instant.now());
    notices.put(notice.id(), notice);
    return notice;
  }

  private LabUser userByToken(String authorization) {
    String token = token(authorization);
    Long userId = sessions.get(token);
    if (userId == null || !users.containsKey(userId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录实验室系统");
    }
    LabUser user = users.get(userId);
    if (!user.active()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "实验室成员权限已被停用");
    }
    return user;
  }

  private LabUser requireAdmin(String authorization) {
    LabUser user = userByToken(authorization);
    if (user.labRole() < 1) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要实验室助教或主教练权限");
    }
    return user;
  }

  private String token(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录实验室系统");
    }
    return authorization.replace("Bearer ", "").trim();
  }

  private LabProblem problem(Long problemId) {
    LabProblem problem = problems.get(problemId);
    if (problem == null || !"published".equals(problem.status())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在或未发布");
    }
    return problem;
  }

  private JudgeResult judge(LabProblem problem, SubmissionRequest request) {
    String language = normalizeLanguage(request.language());
    String standard = problem.standardSolutions().get(language);
    if (standard == null || standard.isBlank()) {
      return new JudgeResult("CE", "该语言暂无参考答案配置，生产环境请接入沙箱判题。", 0);
    }
    boolean accepted = normalizeSource(standard).equals(normalizeSource(request.code()));
    return accepted
        ? new JudgeResult("AC", "安全模式评测通过；真实环境应替换为 Docker/Isolate 沙箱执行。", problem.cases().size())
        : new JudgeResult("WA", "安全模式未执行任意代码；提交与参考解不一致。", 0);
  }

  private void applyAcceptedCheckin(LabUser user, LabProblem problem, LabSubmission submission) {
    if (!LocalDate.now().equals(problem.publishDate())) return;
    boolean existing =
        checkins.values().stream()
            .anyMatch(item -> item.labUserId().equals(user.id()) && item.checkinDate().equals(LocalDate.now()));
    if (existing) return;
    createCheckin(
        user,
        ATTENDANCE_OJ_AC,
        "oj",
        submission.id(),
        0,
        settings.normalClubPoints(),
        "通过每日一练 AC 自动签到，社团积分待同步");
  }

  private LabCheckinLog createCheckin(
      LabUser user,
      int attendanceStatus,
      String sourceType,
      Long sourceId,
      int localScoreChange,
      int clubScoreChange,
      String remark) {
    LabCheckinLog log =
        new LabCheckinLog(
            nextId(),
            user.id(),
            LocalDate.now(),
            attendanceStatus,
            attendanceText(attendanceStatus),
            sourceType,
            sourceId,
            localScoreChange,
            clubScoreChange,
            clubScoreChange > 0 ? 1 : 0,
            clubScoreChange > 0 ? "待同步" : "无需同步",
            remark,
            Instant.now());
    checkins.put(log.id(), log);
    notices.put(nextId(), new LabNotice(nextId(), user.id(), "实验室考勤更新", remark, "checkin", false, Instant.now()));
    return log;
  }

  private void updateReputation(LabUser user, int delta) {
    users.put(
        user.id(),
        new LabUser(
            user.id(),
            user.clubUserId(),
            user.username(),
            user.realName(),
            user.labRole(),
            user.reputationScore() + delta,
            user.active(),
            user.createdAt()));
  }

  private LabProblem defaultProblem() {
    long id = nextId();
    return new LabProblem(
        id,
        "每日一练：A+B 输入输出",
        "## 题目描述\n\n给定两个整数 `a` 和 `b`，输出它们的和。\n\n## 输入格式\n\n一行两个整数。\n\n## 输出格式\n\n输出一个整数。",
        "easy",
        1000,
        128,
        "published",
        LocalDate.now(),
        defaultCases(id),
        defaultSolutions(),
        Instant.now());
  }

  private List<LabProblemCase> defaultCases(long problemId) {
    return List.of(
        new LabProblemCase(nextId(), problemId, "1 2\n", "3\n", true, 1),
        new LabProblemCase(nextId(), problemId, "100 230\n", "330\n", false, 2));
  }

  private Map<String, String> defaultSolutions() {
    Map<String, String> solutions = new LinkedHashMap<>();
    solutions.put(
        "cpp",
        """
        #include <bits/stdc++.h>
        using namespace std;
        int main() {
          long long a, b;
          if (cin >> a >> b) cout << a + b << '\\n';
          return 0;
        }
        """);
    solutions.put(
        "java",
        """
        import java.util.*;
        public class Main {
          public static void main(String[] args) {
            Scanner sc = new Scanner(System.in);
            long a = sc.nextLong();
            long b = sc.nextLong();
            System.out.println(a + b);
          }
        }
        """);
    solutions.put("python", "a, b = map(int, input().split())\nprint(a + b)\n");
    return solutions;
  }

  private void broadcast(String title, String content, String type) {
    notices.put(nextId(), new LabNotice(nextId(), null, title, content, type, false, Instant.now()));
  }

  private String normalizeLanguage(String language) {
    String value = language == null ? "cpp" : language.trim().toLowerCase();
    return switch (value) {
      case "java", "python" -> value;
      default -> "cpp";
    };
  }

  private String normalizeSource(String value) {
    return value == null ? "" : value.replace("\r\n", "\n").trim().replaceAll("\\s+", "");
  }

  private String attendanceText(int status) {
    return switch (status) {
      case ATTENDANCE_OJ_AC -> "刷题自动签到";
      case ATTENDANCE_ONSITE -> "现场签到";
      case ATTENDANCE_LATE -> "迟到";
      default -> "旷课";
    };
  }

  private String blankDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private long nextId() {
    return ids.getAndIncrement();
  }

  private record JudgeResult(String status, String message, int passedCases) {}
}
