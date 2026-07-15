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
@TableName("member_profile_comment")
public class MemberProfileComment {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("profile_user_id")
  private Integer profileUserId;

  @TableField("user_id")
  private Integer userId;

  @TableField("parent_id")
  private Long parentId;

  private String content;
  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
