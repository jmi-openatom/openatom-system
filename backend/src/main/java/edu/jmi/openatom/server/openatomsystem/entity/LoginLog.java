package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录日志
 *
 * <p>对应数据库表 login_log, 记录用户的登录行为, 包括登录时间, IP地址和用户代理信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("login_log")
public class LoginLog {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("user_id")
  private Integer userId;

  @TableField("login_at")
  private Timestamp loginAt;

  private String ip;

  @TableField("user_agent")
  private String userAgent;
}
