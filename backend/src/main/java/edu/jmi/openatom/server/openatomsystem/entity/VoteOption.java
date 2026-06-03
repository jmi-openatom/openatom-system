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

/** 投票选项 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("vote_option")
public class VoteOption {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("vote_id")
  private Integer voteId;

  private String title;

  private String description;

  @TableField("sort_order")
  private Integer sortOrder;

  private String color;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
