package edu.jmi.openatom.lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.lab.entity.Notification;
import edu.jmi.openatom.lab.entity.Problem;
import edu.jmi.openatom.lab.repository.NotificationRepository;
import edu.jmi.openatom.lab.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    private final ProblemRepository problemRepository;
    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${lab.ai.api-key}")
    private String aiApiKey;

    @Value("${lab.ai.api-base-url}")
    private String aiApiBaseUrl;

    @Scheduled(cron = "0 0 0 * * ?") // 每天 00:00
    public void generateDailyProblem() {
        try {
            LocalDate today = LocalDate.now();

            // 检查今日是否已生成
            if (problemRepository.findByProblemDate(today).isPresent()) {
                log.info("今日题目已存在，跳过生成");
                return;
            }

            // 调用 AI 生成题目
            String aiResponse = callAIAPI();
            Problem problem = parseProblemFromAI(aiResponse);
            problem.setProblemDate(today);
            problemRepository.save(problem);

            // 发送群发通知
            Notification notification = new Notification();
            notification.setUserId(null); // 群发
            notification.setTitle("每日算法挑战已更新");
            notification.setContent("今日算法挑战已更新，请及时前往 AC 并完成签到。\n题目：" + problem.getTitle());
            notification.setType("PROBLEM_UPDATE");
            notificationRepository.save(notification);

            log.info("每日题目生成成功: {}", problem.getTitle());
        } catch (Exception e) {
            log.error("生成每日题目失败", e);
        }
    }

    private String callAIAPI() {
        String prompt = """
            生成一道ACM算法题目，返回JSON格式：
            {
              "title": "题目名称",
              "description": "题目描述（Markdown格式）",
              "timeLimit": 1000,
              "memoryLimit": 256,
              "testCases": "[{\\"input\\":\\"1 2\\",\\"output\\":\\"3\\"}]",
              "standardAnswer": "C++标准答案代码"
            }
            """;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        requestBody.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        try {
            // 模拟 AI 返回（实际环境替换为真实 API 调用）
            return """
                {
                  "title": "两数之和",
                  "description": "给定一个整数数组和一个目标值，找出数组中和为目标值的两个数。",
                  "timeLimit": 1000,
                  "memoryLimit": 256,
                  "testCases": "[{\\"input\\":\\"2 7 11 15\\\\n9\\",\\"output\\":\\"0 1\\"}]",
                  "standardAnswer": "// C++ code here"
                }
                """;
        } catch (Exception e) {
            throw new RuntimeException("AI API 调用失败", e);
        }
    }

    private Problem parseProblemFromAI(String json) {
        try {
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            Problem problem = new Problem();
            problem.setTitle((String) data.get("title"));
            problem.setDescription((String) data.get("description"));
            problem.setTimeLimit((Integer) data.get("timeLimit"));
            problem.setMemoryLimit((Integer) data.get("memoryLimit"));
            problem.setTestCases((String) data.get("testCases"));
            problem.setStandardAnswer((String) data.get("standardAnswer"));
            return problem;
        } catch (Exception e) {
            throw new RuntimeException("解析 AI 返回失败", e);
        }
    }
}
