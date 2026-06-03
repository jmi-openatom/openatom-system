package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存应用展示请求
 *
 * <p>用于新增或更新应用展示条目
 */
@Data
public class RequestSaveShowcaseAppDTO {
  @NotBlank(message = "应用名称不能为空")
  private String name;

  private String summary;
  private String coverUrl;
  private Boolean openSource;
  private String githubUrl;
  private String giteeUrl;
  private String developer;
  private String owner;
  private String licenseName;
  private String version;
  private String appStatus;
  private String downloadUrl;
  private String status;
  private Integer sortOrder;
}
