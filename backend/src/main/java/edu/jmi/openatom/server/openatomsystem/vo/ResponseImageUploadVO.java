package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图床上传响应
 *
 * <p>返回图片公开地址及可直接插入博客正文的 Markdown 片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseImageUploadVO {
  private String fileName;
  private String url;
  private String markdown;
}
