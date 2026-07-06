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

/**
 * 用户实体
 *
 * <p>对应数据库表 tb_user, 存储用户基本信息, 包括学号, 姓名, 院系, 专业等个人信息及账户状态
 */
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

  @TableField("miniapp_openid")
  private String miniappOpenid;

  @TableField("wechat_unionid")
  private String wechatUnionid;

  @TableField("qq_openid")
  private String qqOpenid;

  @TableField("user_status")
  private UserStatus userStatus;

  @TableField("create_time")
  private Timestamp createTime;

  @TableField("last_login_at")
  private Timestamp lastLoginAt;

  @TableField("onboarding_completed_at")
  private Timestamp onboardingCompletedAt;

  @TableField("activated_at")
  private Timestamp activatedAt;
}
