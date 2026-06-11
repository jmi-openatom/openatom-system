package edu.jmi.openatom.lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.lab.entity.Problem;
import edu.jmi.openatom.lab.entity.Submission;
import edu.jmi.openatom.lab.repository.ProblemRepository;
import edu.jmi.openatom.lab.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final CheckinService checkinService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${lab.sandbox.mode:safe}")
    private String sandboxMode;

    public Submission judge(Long userId, Long problemId, String code, String language) {
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new RuntimeException("题目不存在"));

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setCode(code);
        submission.setLanguage(language);

        try {
            // 解析测试用例
            List<Map<String, String>> testCases = objectMapper.readValue(
                problem.getTestCases(),
                List.class
            );

            // 执行判题
            JudgeResult result = runInSandbox(code, language, testCases, problem);

            submission.setStatus(result.status);
            submission.setTimeUsed(result.timeUsed);
            submission.setMemoryUsed(result.memoryUsed);
            submission.setErrorMessage(result.errorMessage);

            submissionRepository.save(submission);

            // AC 自动触发签到
            if ("AC".equals(result.status)) {
                checkinService.checkinByProblemAC(userId, problemId);
            }

            return submission;
        } catch (Exception e) {
            submission.setStatus("CE");
            submission.setErrorMessage(e.getMessage());
            submissionRepository.save(submission);
            return submission;
        }
    }

    private JudgeResult runInSandbox(String code, String language, List<Map<String, String>> testCases, Problem problem) {
        // 模拟沙箱执行（生产环境替换为 Docker/Isolate）
        JudgeResult result = new JudgeResult();

        if (code.contains("error")) {
            result.status = "RE";
            result.errorMessage = "Runtime Error";
            return result;
        }

        if (code.length() < 10) {
            result.status = "WA";
            return result;
        }

        result.status = "AC";
        result.timeUsed = 100;
        result.memoryUsed = 10;
        return result;
    }

    static class JudgeResult {
        String status;
        Integer timeUsed;
        Integer memoryUsed;
        String errorMessage;
    }
}
