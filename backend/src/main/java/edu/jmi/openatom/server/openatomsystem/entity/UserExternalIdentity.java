package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.Data;

@Data
@TableName("user_external_identity")
public class UserExternalIdentity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Integer userId;
  private String provider;
  private String subject;
  private String providerUsername;
  private String avatarUrl;
  private Timestamp createdAt;
  private Timestamp lastLoginAt;
}
