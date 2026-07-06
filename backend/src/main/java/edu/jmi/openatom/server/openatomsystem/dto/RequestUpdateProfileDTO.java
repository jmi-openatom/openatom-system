package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户自助更新资料请求
 *
 * <p>用于入社引导或个人中心场景下, 用户维护自己的真实姓名、性别、联系方式及院系班级等基础信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestUpdateProfileDTO {
  @Size(max = 64, message = "真实姓名长度不能超过64个字符")
  private String realName;

  @Size(max = 10, message = "性别长度不能超过10个字符")
  private String gender;

  @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
  private String phone;

  @Email(message = "邮箱格式不正确")
  @Size(max = 128, message = "邮箱长度不能超过128个字符")
  private String email;

  @Size(max = 100, message = "学院长度不能超过100个字符")
  private String college;

  @Size(max = 100, message = "专业长度不能超过100个字符")
  private String major;

  @Size(max = 20, message = "年级长度不能超过20个字符")
  private String grade;

  @Size(max = 100, message = "班级长度不能超过100个字符")
  private String className;
}
