package edu.jmi.openatom.server.openatomsystem.dto.request;

import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateUserDTO {
  @NotBlank(message = "用户名不能为空")
  @Size(max = 64, message = "用户名长度不能超过64个字符")
  private String username;

  @NotBlank(message = "密码不能为空")
  @Size(min = 8, max = 72, message = "密码长度必须在8到72个字符之间")
  private String password;

  @NotBlank(message = "真实姓名不能为空")
  @Size(max = 64, message = "真实姓名长度不能超过64个字符")
  private String realName;

  private String gender;

  @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
  private String phone;

  @Email(message = "邮箱格式不正确")
  @Size(max = 128, message = "邮箱长度不能超过128个字符")
  private String email;

  @Size(max = 32, message = "学号长度不能超过32个字符")
  private String studentNo;
  private String college;
  private String major;
  private String grade;
  private String className;
  private String avatar;
  private UserStatus status;
}
