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
 * 抽奖中奖记录
 *
 * <p>中奖人信息会保存一份快照, 避免后续用户或提交记录变更影响大屏历史
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("lottery_winner")
public class LotteryWinner {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("lottery_id")
  private Integer lotteryId;

  @TableField("prize_id")
  private Integer prizeId;

  @TableField("submission_id")
  private Integer submissionId;

  @TableField("user_id")
  private Integer userId;

  @TableField("winner_name")
  private String winnerName;

  @TableField("winner_contact")
  private String winnerContact;

  @TableField("winner_account")
  private String winnerAccount;

  @TableField("won_at")
  private Timestamp wonAt;
}
