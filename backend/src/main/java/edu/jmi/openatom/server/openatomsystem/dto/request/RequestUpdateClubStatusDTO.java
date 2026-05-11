package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新社团状态请求
 *
 * <p>用于更新社团的运营状态, 携带状态值status（active或inactive）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateClubStatusDTO {
  @NotBlank(message = "社团状态不能为空")
  @Pattern(regexp = "active|inactive", message = "社团状态只能是active或inactive")
  private String status;
}
