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
 * 抽奖奖品配置
 *
 * <p>quantity 表示当前奖项可抽出的总数量, 已抽数量从中奖记录计算
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("lottery_prize")
public class LotteryPrize {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("lottery_id")
  private Integer lotteryId;

  private String name;

  private String level;

  private Integer quantity;

  @TableField("sort_order")
  private Integer sortOrder;

  private String color;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
