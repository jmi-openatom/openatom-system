package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class RequestBatchUpdateCommentStatusDTO {
  @NotEmpty(message = "请选择评论")
  private List<Long> commentIds;

  @NotBlank(message = "评论状态不能为空")
  private String status;
}
