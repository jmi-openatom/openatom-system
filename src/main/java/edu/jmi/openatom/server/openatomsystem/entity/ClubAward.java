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
@TableName("club_award")
public class ClubAward {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  private String title;

  @TableField("competition_name")
  private String competitionName;

  @TableField("award_level")
  private String awardLevel;

  @TableField("award_year")
  private Integer awardYear;

  @TableField("team_name")
  private String teamName;

  private String description;

  @TableField("sort_order")
  private Integer sortOrder;

  @TableField("created_at")
  private Timestamp createdAt;
}
