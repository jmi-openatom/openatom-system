package edu.jmi.openatom.server.openatomsystem.service;

/**
 * 站点地图服务接口
 *
 * <p>生成供搜索引擎收录的 sitemap.xml, 包含静态页面与已发布动态内容(博客文章, 活动, 制度, 应用)的 URL
 */
public interface SitemapService {

  /** 生成 sitemap.xml 文档内容 */
  String generateSitemapXml();
}