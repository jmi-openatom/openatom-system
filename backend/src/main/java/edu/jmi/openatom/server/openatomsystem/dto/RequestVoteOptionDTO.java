package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 投票选项请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestVoteOptionDTO {
  private Integer id;

  @NotBlank(message = "选项标题不能为空")
  private String title;

  private String description;
  private Integer sortOrder;
  private String color;
}
