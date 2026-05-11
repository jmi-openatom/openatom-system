package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建招新计划请求
 *
 * <p>用于创建招新活动, 包含计划名称name, 申请起止时间applyStartAt和applyEndAt, 面试时间范围interviewStartAt和interviewEndAt, 结果发布时间resultPublishAt, 目标年级targetGrades, 最大申请人数maxApplicants, 是否要求登录loginRequired, 报名表单结构formSchema和状态status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateRecruitmentCampaignDTO {
  @NotBlank(message = "招新计划名称不能为空")
  private String name;

  @NotBlank(message = "申请开始时间不能为空")
  private String applyStartAt;

  @NotBlank(message = "申请结束时间不能为空")
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
