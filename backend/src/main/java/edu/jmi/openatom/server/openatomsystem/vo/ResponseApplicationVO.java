package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 申请响应VO
 *
 * <p>包含申请ID, 志愿部门信息, 申请人资料及申请状态, 用于展示用户的社团申请记录
 */
@Data
@Builder
public class ResponseApplicationVO {
  private Integer id;
  private Integer campaignId;
  private String campaignName;
  private Integer clubId;
  private String clubName;
  private Integer userId;
  private String applicantName;
  private String studentId;
  private Integer firstChoiceDepartmentId;
  private String firstChoiceDepartmentName;
  private Integer secondChoiceDepartmentId;
  private String secondChoiceDepartmentName;
  private String preferredDepartment;
  private String status;
  private Map<String, Object> profile;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
