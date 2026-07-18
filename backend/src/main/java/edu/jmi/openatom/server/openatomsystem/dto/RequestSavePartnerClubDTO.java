package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 新增或编辑开源伙伴。 */
@Data
public class RequestSavePartnerClubDTO {
  @NotBlank(message = "伙伴名称不能为空")
  @Size(max = 50, message = "伙伴名称不能超过 50 个字符")
  private String name;

  @NotBlank(message = "Logo 地址不能为空")
  @Size(max = 500, message = "Logo 地址不能超过 500 个字符")
  private String logoUrl;

  @NotBlank(message = "伙伴简介不能为空")
  @Size(max = 300, message = "伙伴简介不能超过 300 个字符")
  private String description;

  @Size(max = 500, message = "官网地址不能超过 500 个字符")
  private String websiteUrl;

  @Size(max = 160, message = "所属组织不能超过 160 个字符")
  private String organization;

  @Size(max = 80, message = "伙伴类型不能超过 80 个字符")
  private String category;

  private Integer presidentUserId;

  private List<@Size(max = 30, message = "单个标签不能超过 30 个字符") String> tags;
  private Integer sortOrder;
  private Boolean featured;
  private String status;
}
