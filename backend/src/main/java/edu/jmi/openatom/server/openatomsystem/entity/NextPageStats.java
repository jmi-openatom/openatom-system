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
 * Next 页面统计
 *
 * <p>对应数据库表 next_page_stats, 存储 openatom-system-next 页面的访问量和点赞数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("next_page_stats")
public class NextPageStats {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("view_count")
  private Integer viewCount;

  @TableField("like_count")
  private Integer likeCount;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
