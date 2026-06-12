package edu.jmi.openatom.lab.framework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_users")
public class LabUser {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long clubUserId;
  private String username;
  private String nickname;
  private String avatarUrl;
  private String email;
  private String phone;
  private Integer labRole;
  private Integer reputationScore;
  private Boolean disabled;
  private LocalDateTime lastLoginAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
