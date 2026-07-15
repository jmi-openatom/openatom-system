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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_profile_like")
public class MemberProfileLike {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("profile_user_id")
  private Integer profileUserId;

  @TableField("user_id")
  private Integer userId;

  @TableField("created_at")
  private Timestamp createdAt;
}
