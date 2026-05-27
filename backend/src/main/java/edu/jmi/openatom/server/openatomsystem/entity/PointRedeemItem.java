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

/** 积分兑换商品 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("point_redeem_item")
public class PointRedeemItem {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String name;
  private String description;

  @TableField("point_cost")
  private Integer pointCost;

  private Integer stock;

  @TableField("exchanged_count")
  private Integer exchangedCount;

  private String status;

  @TableField("sort_order")
  private Integer sortOrder;

  @TableField("image_url")
  private String imageUrl;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
