package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新招新计划请求
 *
 * <p>用于更新招新活动配置, 包含计划名称name, 申请起止时间applyStartAt和applyEndAt, 面试时间范围interviewStartAt和interviewEndAt, 结果发布时间resultPublishAt, 目标年级targetGrades, 最大申请人数maxApplicants, 是否要求登录loginRequired, 报名表单结构formSchema和状态status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateRecruitmentCampaignDTO {
  private String name;
  private String applyStartAt;
  private String applyEndAt;
  private String interviewStartAt;
  private String interviewEndAt;
  private String resultPublishAt;
  private List<String> targetGrades;

  @Min(value = 1, message = "申请人数上限必须大于0")
  private Integer maxApplicants;

  private Boolean loginRequired;
  private List<Map<String, Object>> formSchema;

  private String status;
}
