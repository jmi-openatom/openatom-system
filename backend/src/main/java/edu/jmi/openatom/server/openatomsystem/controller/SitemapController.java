package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.service.SitemapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点地图控制器
 *
 * <p>提供搜索引擎抓取的 sitemap.xml, 由前端 nginx 将 /sitemap.xml 反向代理到此接口
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {

  private final SitemapService sitemapService;

  @GetMapping(value = "/site/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public String sitemapXml() {
    return sitemapService.generateSitemapXml();
  }
}