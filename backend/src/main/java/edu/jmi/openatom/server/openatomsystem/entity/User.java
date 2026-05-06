package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_user")
public class User {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("user_name")
  private String userName;

  private String password;

  @TableField("real_name")
  private String realName;

  private String gender;
  private String phone;
  private String email;

  @TableField("student_id")
  private String studentId;

  private String college;
  private String major;
  private String grade;

  @TableField("class_name")
  private String className;

  private String avatar;

  @TableField("user_status")
  private UserStatus userStatus;

  @TableField("create_time")
  private Timestamp createTime;

  @TableField("last_login_at")
  private Timestamp lastLoginAt;
}
