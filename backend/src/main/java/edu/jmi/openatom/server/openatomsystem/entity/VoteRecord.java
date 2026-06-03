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

/** 投票记录 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("vote_record")
public class VoteRecord {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("vote_id")
  private Integer voteId;

  @TableField("user_id")
  private Integer userId;

  @TableField("voter_name")
  private String voterName;

  @TableField("voter_contact")
  private String voterContact;

  @TableField("voter_key")
  private String voterKey;

  @TableField("selected_option_ids")
  private String selectedOptionIds;

  private String remark;

  @TableField("voted_at")
  private Timestamp votedAt;
}
