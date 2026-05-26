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
 * 抽奖活动
 *
 * <p>关联一个站点表单作为参与名单来源, 管理抽奖状态和大屏展示基础信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("lottery_campaign")
public class LotteryCampaign {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("form_id")
  private Integer formId;

  private String title;

  private String description;

  private String status;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
