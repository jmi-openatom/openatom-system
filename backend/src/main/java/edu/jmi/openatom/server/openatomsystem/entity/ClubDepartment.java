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
 * 社团部门
 *
 * <p>对应数据库表 club_department, 存储社团内部的部门信息, 包括部门名称, 描述及负责人
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("club_department")
public class ClubDepartment {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  private String name;
  private String description;

  @TableField("manager_user_id")
  private Integer managerUserId;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
