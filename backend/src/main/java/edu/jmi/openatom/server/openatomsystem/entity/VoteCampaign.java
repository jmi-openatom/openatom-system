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

/** 投票活动 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("vote_campaign")
public class VoteCampaign {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  private String title;

  private String description;

  private String status;

  @TableField("vote_type")
  private String voteType;

  @TableField("max_choices")
  private Integer maxChoices;

  @TableField("anonymous_allowed")
  private Boolean anonymousAllowed;

  @TableField("result_visible")
  private Boolean resultVisible;

  @TableField("result_visibility")
  private String resultVisibility;

  @TableField("start_at")
  private Timestamp startAt;

  @TableField("end_at")
  private Timestamp endAt;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
