package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.entity.BlogArticle;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.ClubRegulation;
import edu.jmi.openatom.server.openatomsystem.entity.ShowcaseApp;
import edu.jmi.openatom.server.openatomsystem.mapper.BlogArticleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubActivityMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubRegulationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ShowcaseAppMapper;
import edu.jmi.openatom.server.openatomsystem.service.SitemapService;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 站点地图实现类
 *
 * <p>聚合公开静态页面与已发布内容生成 sitemap.xml
 */
@Service
@RequiredArgsConstructor
public class SitemapServiceImpl implements SitemapService {

  private static final DateTimeFormatter LAST_MOD_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

  private final BlogArticleMapper blogArticleMapper;
  private final ClubActivityMapper clubActivityMapper;
  private final ClubRegulationMapper clubRegulationMapper;
  private final ShowcaseAppMapper showcaseAppMapper;

  @Value("${app.site.base-url:https://www.jmi-openatom.cn}")
  private String baseUrl;

  @Override
  public String generateSitemapXml() {
    StringBuilder xml = new StringBuilder(4096);
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    xml.append("  <url><loc>").append(baseUrl).append("/</loc><priority>1.0</priority><changefreq>daily</changefreq></url>\n");

    staticEntry(xml, "/about", "0.9", "monthly");
    staticEntry(xml, "/activities", "0.8", "daily");
    staticEntry(xml, "/apply", "0.8", "weekly");
    staticEntry(xml, "/blog", "0.8", "daily");
    staticEntry(xml, "/apps", "0.7", "weekly");
    staticEntry(xml, "/partners", "0.7", "monthly");
    staticEntry(xml, "/regulations", "0.6", "monthly");
    staticEntry(xml, "/open-platform", "0.6", "monthly");
    staticEntry(xml, "/votes", "0.6", "weekly");
    staticEntry(xml, "/points", "0.5", "weekly");
    staticEntry(xml, "/calendar", "0.5", "weekly");
    staticEntry(xml, "/alumni-managers", "0.5", "monthly");
    staticEntry(xml, "/next", "0.5", "weekly");

    appendPublishedArticles(xml);
    appendPublishedActivities(xml);
    appendPublishedRegulations(xml);
    appendPublishedApps(xml);

    xml.append("</urlset>\n");
    return xml.toString();
  }

  private void staticEntry(StringBuilder xml, String path, String priority, String changefreq) {
    xml.append("  <url><loc>").append(baseUrl).append(path)
        .append("</loc><priority>").append(priority)
        .append("</priority><changefreq>").append(changefreq).append("</changefreq></url>\n");
  }

  private void appendPublishedArticles(StringBuilder xml) {
    List<BlogArticle> articles =
        blogArticleMapper.selectList(
            new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getUpdatedAt)
                .eq(BlogArticle::getStatus, "published")
                .orderByDesc(BlogArticle::getUpdatedAt));
    for (BlogArticle article : articles) {
      dynamicEntry(xml, "/blog/" + article.getId(), "0.7", article.getUpdatedAt());
    }
  }

  private void appendPublishedActivities(StringBuilder xml) {
    List<ClubActivity> activities =
        clubActivityMapper.selectList(
            new LambdaQueryWrapper<ClubActivity>()
                .select(ClubActivity::getId, ClubActivity::getUpdatedAt)
                .eq(ClubActivity::getStatus, "published")
                .orderByDesc(ClubActivity::getUpdatedAt));
    for (ClubActivity activity : activities) {
      dynamicEntry(xml, "/activities/" + activity.getId(), "0.6", activity.getUpdatedAt());
    }
  }

  private void appendPublishedRegulations(StringBuilder xml) {
    List<ClubRegulation> regulations =
        clubRegulationMapper.selectList(
            new LambdaQueryWrapper<ClubRegulation>()
                .select(ClubRegulation::getId, ClubRegulation::getUpdatedAt)
                .eq(ClubRegulation::getStatus, "published")
                .orderByDesc(ClubRegulation::getUpdatedAt));
    for (ClubRegulation regulation : regulations) {
      dynamicEntry(xml, "/regulations/" + regulation.getId(), "0.5", regulation.getUpdatedAt());
    }
  }

  private void appendPublishedApps(StringBuilder xml) {
    List<ShowcaseApp> apps =
        showcaseAppMapper.selectList(
            new LambdaQueryWrapper<ShowcaseApp>()
                .select(ShowcaseApp::getId, ShowcaseApp::getUpdatedAt)
                .eq(ShowcaseApp::getStatus, "published")
                .orderByDesc(ShowcaseApp::getUpdatedAt));
    for (ShowcaseApp app : apps) {
      dynamicEntry(xml, "/apps/" + app.getId(), "0.6", app.getUpdatedAt());
    }
  }

  private void dynamicEntry(StringBuilder xml, String path, String priority, Timestamp updatedAt) {
    xml.append("  <url><loc>").append(baseUrl).append(path)
        .append("</loc><lastmod>").append(formatLastMod(updatedAt))
        .append("</lastmod><priority>").append(priority)
        .append("</priority><changefreq>weekly</changefreq></url>\n");
  }

  private String formatLastMod(Timestamp timestamp) {
    if (timestamp == null) return "";
    return LAST_MOD_FORMATTER.format(timestamp.toInstant().atZone(CHINA_ZONE).toLocalDate());
  }
}