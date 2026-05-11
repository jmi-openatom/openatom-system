package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表单提交记录DTO
 *
 * <p>包含表单提交ID, 所属表单, 提交者信息, 联系方式以及表单填写的具体数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseFormSubmissionDTO {
  private Integer id;
  private Integer formId;
  private Integer clubId;
  private Integer userId;
  private String submitterName;
  private String submitterAccount;
  private String contact;
  private Map<String, Object> formData;
  private Timestamp createdAt;
}
