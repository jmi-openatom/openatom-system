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
 * 社团活动
 *
 * <p>对应数据库表 club_activity, 存储社团发布的活动信息, 包括活动时间, 地点, 报名设置及内容描述
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("club_activity")
public class ClubActivity {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  private String title;
  private String summary;

  @TableField("description_markdown")
  private String descriptionMarkdown;

  @TableField("activity_at")
  private Timestamp activityAt;

  @TableField("end_at")
  private Timestamp endAt;

  private String location;
  private String status;

  @TableField("cover_url")
  private String coverUrl;

  @TableField("registration_required")
  private Boolean registrationRequired;

  @TableField("registration_start_at")
  private Timestamp registrationStartAt;

  @TableField("registration_end_at")
  private Timestamp registrationEndAt;

  @TableField("registration_fields")
  private String registrationFields;

  @TableField("participation_points")
  private Long participationPoints;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
