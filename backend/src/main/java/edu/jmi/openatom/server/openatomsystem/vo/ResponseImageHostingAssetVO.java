package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图床资产响应
 *
 * <p>用于前台图床列表和后台图床管理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseImageHostingAssetVO {
  private Integer id;
  private Integer uploaderId;
  private String uploaderName;
  private String uploaderAvatar;
  private String fileName;
  private String originalName;
  private String contentType;
  private Long fileSize;
  private String url;
  private String markdown;
  private String status;
  private Timestamp deletedAt;
  private Timestamp createdAt;
}
