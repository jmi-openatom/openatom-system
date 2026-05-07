package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateNotificationDTO {
  @NotBlank(message = "通知标题不能为空")
  private String title;

  @NotBlank(message = "通知内容不能为空")
  private String content;

  private String type;

  private Boolean isAll;

  private List<Integer> receiverUserIds;
}
