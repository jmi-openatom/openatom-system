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
@AllArgsConstructor
@NoArgsConstructor
@TableName("checkin_group_member")
public class CheckInGroupMember {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("group_id")
  private Integer groupId;

  @TableField("user_id")
  private Integer userId;

  @TableField("created_at")
  private Timestamp createdAt;
}
