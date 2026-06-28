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
 * 往届管理人员分组
 *
 * <p>对应数据库表 club_alumni_group, 存储往届管理人员的分组信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("club_alumni_group")
public class ClubAlumniGroup {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  private String name;
  private String description;

  @TableField("sort_order")
  private Integer sortOrder;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
