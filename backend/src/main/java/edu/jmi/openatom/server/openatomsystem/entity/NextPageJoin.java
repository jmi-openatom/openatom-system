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
 * Next 页面加入开发申请
 *
 * <p>对应数据库表 next_page_join, 存储用户提交加入开发的表单信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("next_page_join")
public class NextPageJoin {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("name")
  private String name;

  @TableField("contact")
  private String contact;

  @TableField("direction")
  private String direction;

  @TableField("skills")
  private String skills;

  @TableField("message")
  private String message;

  @TableField("created_at")
  private Timestamp createdAt;
}
