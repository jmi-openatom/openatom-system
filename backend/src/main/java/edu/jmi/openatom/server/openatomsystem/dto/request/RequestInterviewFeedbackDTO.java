package edu.jmi.openatom.server.openatomsystem.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试反馈请求
 *
 * <p>用于面试官提交面试评价, 包含各项评分scores, 录用建议suggestion和评语comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestInterviewFeedbackDTO {
  private Map<String, Object> scores;
  private String suggestion;
  private String comment;
}
