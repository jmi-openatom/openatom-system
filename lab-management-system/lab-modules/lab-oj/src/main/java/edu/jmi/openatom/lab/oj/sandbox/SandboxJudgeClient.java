package edu.jmi.openatom.lab.oj.sandbox;

import edu.jmi.openatom.lab.framework.properties.LabSandboxProperties;
import edu.jmi.openatom.lab.oj.sandbox.SandboxJudgeModels.JudgeRequest;
import edu.jmi.openatom.lab.oj.sandbox.SandboxJudgeModels.JudgeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SandboxJudgeClient {
  private final LabSandboxProperties properties;
  private final RestClient restClient;

  public JudgeResult judge(JudgeRequest request) {
    if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
      return localDevelopmentJudge(request);
    }
    return restClient.post().uri(properties.getEndpoint()).body(request).retrieve().body(JudgeResult.class);
  }

  private JudgeResult localDevelopmentJudge(JudgeRequest request) {
    int total = request.cases() == null ? 0 : request.cases().size();
    if (request.code() == null || request.code().isBlank()) {
      return new JudgeResult("CE", 0, total, 0, 0, "代码不能为空");
    }
    boolean accepted = request.code().contains("__LAB_ACCEPT__");
    return new JudgeResult(
        accepted ? "AC" : "WA",
        accepted ? total : 0,
        total,
        accepted ? 1 : 0,
        0,
        accepted ? "开发模式判题通过" : "未配置沙箱时，代码需包含 __LAB_ACCEPT__ 才会模拟 AC");
  }
}
