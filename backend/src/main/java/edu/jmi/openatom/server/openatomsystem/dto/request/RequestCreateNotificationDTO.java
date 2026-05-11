package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建通知请求
 *
 * <p>用于发送系统通知, 包含通知标题title, 内容content, 类型type, 是否全员发送isAll以及指定接收者ID列表receiverUserIds
 */
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
