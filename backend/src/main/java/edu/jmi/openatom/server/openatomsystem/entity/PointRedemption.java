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

/** 积分兑换订单 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("point_redemption")
public class PointRedemption {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("user_id")
  private Integer userId;

  @TableField("item_id")
  private Integer itemId;

  private Long points;
  private String status;

  @TableField("receiver_name")
  private String receiverName;

  @TableField("receiver_contact")
  private String receiverContact;

  private String remark;

  @TableField("admin_note")
  private String adminNote;

  @TableField("reviewed_by")
  private Integer reviewedBy;

  @TableField("reviewed_at")
  private Timestamp reviewedAt;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
