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
 * 社团职位
 *
 * <p>对应数据库表 club_position, 定义社团或部门内的职位, 包括职位名称和最大人数限制
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("club_position")
public class ClubPosition {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("department_id")
  private Integer departmentId;

  private String name;

  @TableField("max_count")
  private Integer maxCount;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
