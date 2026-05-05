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
@TableName("club")
public class Club {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String name;
  private String code;
  private String category;
  private String description;

  @TableField("logo_url")
  private String logoUrl;

  @TableField("president_user_id")
  private Integer presidentUserId;

  private String status;

  @TableField("recruitment_status")
  private String recruitmentStatus;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
