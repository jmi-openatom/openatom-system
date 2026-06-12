package edu.jmi.openatom.lab.oj.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.framework.auth.LabSecurityContext;
import edu.jmi.openatom.lab.framework.properties.LabAiProperties;
import edu.jmi.openatom.lab.notice.service.NoticeService;
import edu.jmi.openatom.lab.oj.entity.LabProblem;
import edu.jmi.openatom.lab.oj.entity.LabProblemTestCase;
import edu.jmi.openatom.lab.oj.entity.LabSubmission;
import edu.jmi.openatom.lab.oj.mapper.LabProblemMapper;
import edu.jmi.openatom.lab.oj.mapper.LabProblemTestCaseMapper;
import edu.jmi.openatom.lab.oj.mapper.LabSubmissionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class OjService {
  private final LabProblemMapper problemMapper;
  private final LabProblemTestCaseMapper testCaseMapper;
  private final LabSubmissionMapper submissionMapper;
  private final OjJudgeService judgeService;
  private final LabAiProperties aiProperties;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final NoticeService noticeService;

  public LabDtos.ProblemView today() {
    LabProblem problem = problemMapper.selectByDate(LocalDate.now());
    if (problem == null) {
      problem = generateForDate(LocalDate.now(), true);
    }
    return toProblemView(problem, true);
  }

  public List<LabDtos.ProblemView> recentProblems() {
    return problemMapper.selectRecent(30).stream().map(problem -> toProblemView(problem, true)).toList();
  }

  @Transactional(rollbackFor = Exception.class)
  public LabDtos.ProblemView saveProblem(LabDtos.AdminProblemRequest request) {
    LabProblem problem = problemMapper.selectByDate(request.problemDate());
    boolean insert = problem == null;
    if (insert) {
      problem = new LabProblem();
      problem.setProblemDate(request.problemDate());
      problem.setCreatedAt(LocalDateTime.now());
    }
    problem.setTitle(request.title().trim());
    problem.setSlug(slug(request.title(), request.problemDate()));
    problem.setDifficulty(firstNonBlank(request.difficulty(), "入门"));
    problem.setDescriptionMarkdown(request.descriptionMarkdown());
    problem.setTimeLimitMs(request.timeLimitMs() == null ? 1000 : request.timeLimitMs());
    problem.setMemoryLimitMb(request.memoryLimitMb() == null ? 128 : request.memoryLimitMb());
    problem.setStatus("PUBLISHED");
    problem.setAiGenerated(false);
    problem.setSolutionCpp(request.solutionCpp());
    problem.setSolutionJava(request.solutionJava());
    problem.setSolutionPython(request.solutionPython());
    problem.setUpdatedAt(LocalDateTime.now());
    if (insert) {
      problemMapper.insert(problem);
    } else {
      problemMapper.updateById(problem);
      testCaseMapper.deleteByProblemId(problem.getId());
    }
    saveTestCases(problem.getId(), request.testCases());
    return toProblemView(problem, true);
  }

  public LabDtos.SubmissionView submit(LabDtos.SubmitCodeRequest request) {
    LabProblem problem = problemMapper.selectById(request.problemId());
    if (problem == null || !"PUBLISHED".equals(problem.getStatus())) {
      throw new IllegalArgumentException("题目不存在或未发布");
    }
    LabSubmission submission = new LabSubmission();
    submission.setUserId(LabSecurityContext.userId());
    submission.setProblemId(problem.getId());
    submission.setLanguage(normalizeLanguage(request.language()));
    submission.setCode(request.code());
    submission.setJudgeStatus("PENDING");
    submission.setScorePassed(0);
    submission.setTotalCases(testCaseMapper.selectByProblemId(problem.getId()).size());
    submission.setSubmittedAt(LocalDateTime.now());
    submissionMapper.insert(submission);
    judgeService.submitForJudge(submission.getId());
    return toSubmissionView(submission);
  }

  public List<LabDtos.SubmissionView> mySubmissions() {
    return submissionMapper.selectByUser(LabSecurityContext.userId(), 60).stream().map(this::toSubmissionView).toList();
  }

  public LabDtos.SubmissionView submission(Long id) {
    LabSubmission submission = submissionMapper.selectById(id);
    if (submission == null || !submission.getUserId().equals(LabSecurityContext.userId())) {
      throw new IllegalArgumentException("提交记录不存在");
    }
    return toSubmissionView(submission);
  }

  @Transactional(rollbackFor = Exception.class)
  public LabDtos.ProblemView generateToday() {
    return toProblemView(generateForDate(LocalDate.now(), false), true);
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Shanghai")
  public void scheduledGenerateDailyProblem() {
    try {
      generateForDate(LocalDate.now(), false);
    } catch (RuntimeException ex) {
      log.warn("Daily lab problem generation failed: {}", ex.getMessage());
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public LabProblem generateForDate(LocalDate date, boolean onlyIfAbsent) {
    LabProblem existing = problemMapper.selectByDate(date);
    if (existing != null && onlyIfAbsent) {
      return existing;
    }
    LabDtos.AdminProblemRequest generated = generateByAiOrFallback(date);
    LabDtos.ProblemView view = saveProblem(generated);
    noticeService.broadcastTemplate(
        "PROBLEM_REFRESH",
        "今日算法挑战已更新",
        "今日算法挑战「" + view.title() + "」已发布，请及时前往 AC 并完成签到。");
    return problemMapper.selectById(view.id());
  }

  @SuppressWarnings("unchecked")
  private LabDtos.AdminProblemRequest generateByAiOrFallback(LocalDate date) {
    if (!aiProperties.isEnabled() || aiProperties.getEndpoint() == null || aiProperties.getEndpoint().isBlank()) {
      return fallbackProblem(date);
    }
    try {
      Map<String, Object> body =
          Map.of(
              "model",
              aiProperties.getModel(),
              "messages",
              List.of(
                  Map.of(
                      "role",
                      "user",
                      "content",
                      "生成一题 ACM 每日一练，严格返回 JSON：title,difficulty,descriptionMarkdown,timeLimitMs,memoryLimitMb,testCases[{inputText,expectedOutput,sampleCase,sortOrder}],solutionCpp,solutionJava,solutionPython")));
      Map<String, Object> response =
          restClient
              .post()
              .uri(aiProperties.getEndpoint())
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + aiProperties.getApiKey())
              .body(body)
              .retrieve()
              .body(Map.class);
      Object content = response == null ? null : response.get("content");
      if (content == null && response != null) {
        content = response.get("text");
      }
      if (content == null) {
        return fallbackProblem(date);
      }
      Map<String, Object> problem = objectMapper.readValue(String.valueOf(content), Map.class);
      return fromAiMap(date, problem);
    } catch (Exception ex) {
      log.warn("AI problem generation fallback: {}", ex.getMessage());
      return fallbackProblem(date);
    }
  }

  @SuppressWarnings("unchecked")
  private LabDtos.AdminProblemRequest fromAiMap(LocalDate date, Map<String, Object> problem) {
    List<Map<String, Object>> cases = (List<Map<String, Object>>) problem.get("testCases");
    List<LabDtos.TestCaseInput> testCases =
        cases == null
            ? List.of()
            : cases.stream()
                .map(
                    item ->
                        new LabDtos.TestCaseInput(
                            String.valueOf(item.get("inputText")),
                            String.valueOf(item.get("expectedOutput")),
                            Boolean.TRUE.equals(item.get("sampleCase")),
                            item.get("sortOrder") instanceof Number number ? number.intValue() : 0))
                .toList();
    return new LabDtos.AdminProblemRequest(
        date,
        String.valueOf(problem.get("title")),
        String.valueOf(problem.getOrDefault("difficulty", "中等")),
        String.valueOf(problem.get("descriptionMarkdown")),
        number(problem.get("timeLimitMs"), 1000),
        number(problem.get("memoryLimitMb"), 128),
        testCases,
        string(problem.get("solutionCpp")),
        string(problem.get("solutionJava")),
        string(problem.get("solutionPython")));
  }

  private LabDtos.AdminProblemRequest fallbackProblem(LocalDate date) {
    return new LabDtos.AdminProblemRequest(
        date,
        "两数之和校验",
        "入门",
        """
        ## 题目描述

        给定两个整数 `a` 和 `b`，输出它们的和。

        ## 输入格式

        一行两个整数。

        ## 输出格式

        输出一个整数表示答案。

        ## 开发模式说明

        未配置沙箱时，提交代码中包含 `__LAB_ACCEPT__` 会模拟 AC，用于验证签到联动。
        """,
        1000,
        128,
        List.of(
            new LabDtos.TestCaseInput("1 2\n", "3\n", true, 0),
            new LabDtos.TestCaseInput("20 22\n", "42\n", false, 1)),
        "#include <bits/stdc++.h>\nusing namespace std; int main(){long long a,b;cin>>a>>b;cout<<a+b<<'\\n';}\n",
        "import java.util.*; public class Main { public static void main(String[] args){ Scanner sc=new Scanner(System.in); System.out.println(sc.nextLong()+sc.nextLong()); }}\n",
        "a,b=map(int,input().split());print(a+b)\n");
  }

  private void saveTestCases(Long problemId, List<LabDtos.TestCaseInput> inputs) {
    List<LabDtos.TestCaseInput> values =
        inputs == null || inputs.isEmpty() ? fallbackProblem(LocalDate.now()).testCases() : inputs;
    int order = 0;
    for (LabDtos.TestCaseInput input : values) {
      LabProblemTestCase testCase = new LabProblemTestCase();
      testCase.setProblemId(problemId);
      testCase.setInputText(input.inputText());
      testCase.setExpectedOutput(input.expectedOutput());
      testCase.setSampleCase(Boolean.TRUE.equals(input.sampleCase()));
      testCase.setSortOrder(input.sortOrder() == null ? order++ : input.sortOrder());
      testCaseMapper.insert(testCase);
    }
  }

  private LabDtos.ProblemView toProblemView(LabProblem problem, boolean onlySamples) {
    List<LabDtos.TestCaseView> cases =
        testCaseMapper.selectByProblemId(problem.getId()).stream()
            .filter(testCase -> !onlySamples || Boolean.TRUE.equals(testCase.getSampleCase()))
            .map(
                testCase ->
                    new LabDtos.TestCaseView(
                        testCase.getId(),
                        testCase.getInputText(),
                        testCase.getExpectedOutput(),
                        Boolean.TRUE.equals(testCase.getSampleCase()),
                        testCase.getSortOrder()))
            .toList();
    return new LabDtos.ProblemView(
        problem.getId(),
        problem.getProblemDate(),
        problem.getTitle(),
        problem.getSlug(),
        problem.getDifficulty(),
        problem.getDescriptionMarkdown(),
        problem.getTimeLimitMs(),
        problem.getMemoryLimitMb(),
        problem.getStatus(),
        Boolean.TRUE.equals(problem.getAiGenerated()),
        cases);
  }

  private LabDtos.SubmissionView toSubmissionView(LabSubmission submission) {
    return new LabDtos.SubmissionView(
        submission.getId(),
        submission.getUserId(),
        submission.getProblemId(),
        submission.getLanguage(),
        submission.getJudgeStatus(),
        submission.getScorePassed(),
        submission.getTotalCases(),
        submission.getRuntimeMs(),
        submission.getMemoryKb(),
        submission.getErrorMessage(),
        submission.getSubmittedAt(),
        submission.getJudgedAt());
  }

  private String slug(String title, LocalDate date) {
    return date + "-" + title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-");
  }

  private String normalizeLanguage(String language) {
    String value = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
    if (!List.of("cpp", "java", "python").contains(value)) {
      throw new IllegalArgumentException("暂不支持该语言");
    }
    return value;
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private Integer number(Object value, int fallback) {
    return value instanceof Number number ? number.intValue() : fallback;
  }

  private String string(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
