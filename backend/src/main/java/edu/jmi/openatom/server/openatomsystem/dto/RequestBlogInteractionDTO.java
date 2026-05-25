package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.Data;

/**
 * 博客互动请求
 *
 * <p>分享记录可携带来源渠道, 点赞和收藏可复用该结构
 */
@Data
public class RequestBlogInteractionDTO {
  private String channel;
}
