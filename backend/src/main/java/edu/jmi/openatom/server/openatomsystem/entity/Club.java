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
 * 社团实体
 *
 * <p>对应数据库表 club, 存储社团基本信息, 包括名称, 编码, 分类, 状态及社长信息
 */
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

  @TableField("vice_president_user_id")
  private Integer vicePresidentUserId;

  @TableField("league_secretary_user_id")
  private Integer leagueSecretaryUserId;

  @TableField("wechat_group_qrcode")
  private String wechatGroupQrcode;

  @TableField("qq_group_number")
  private String qqGroupNumber;

  private String status;

  @TableField("recruitment_status")
  private String recruitmentStatus;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
