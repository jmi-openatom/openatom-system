package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.ShowcaseApp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用展示数据访问层
 *
 * <p>提供公开展示列表和后台管理分页查询
 */
@Mapper
public interface ShowcaseAppMapper extends BaseMapper<ShowcaseApp> {

  default Page<ShowcaseApp> selectPageByConditions(
      Page<ShowcaseApp> page, String keyword, String status, Boolean openSource) {
    LambdaQueryWrapper<ShowcaseApp> wrapper = baseWrapper(keyword, status, openSource);
    wrapper.orderByAsc(ShowcaseApp::getSortOrder).orderByDesc(ShowcaseApp::getId);
    return selectPage(page, wrapper);
  }

  default List<ShowcaseApp> selectPublished(String keyword, Boolean openSource) {
    LambdaQueryWrapper<ShowcaseApp> wrapper = baseWrapper(keyword, "published", openSource);
    wrapper.orderByAsc(ShowcaseApp::getSortOrder).orderByDesc(ShowcaseApp::getId);
    return selectList(wrapper);
  }

  default ShowcaseApp selectPublishedById(Integer appId) {
    if (appId == null) return null;
    return selectOne(
        new LambdaQueryWrapper<ShowcaseApp>()
            .eq(ShowcaseApp::getId, appId)
            .eq(ShowcaseApp::getStatus, "published")
            .last("LIMIT 1"));
  }

  private LambdaQueryWrapper<ShowcaseApp> baseWrapper(
      String keyword, String status, Boolean openSource) {
    LambdaQueryWrapper<ShowcaseApp> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(status != null && !status.isBlank(), ShowcaseApp::getStatus, status)
        .eq(openSource != null, ShowcaseApp::getOpenSource, openSource)
        .and(
            keyword != null && !keyword.isBlank(),
            query -> {
              String trimmed = keyword.trim();
              query
                  .like(ShowcaseApp::getName, trimmed)
                  .or()
                  .like(ShowcaseApp::getSummary, trimmed)
                  .or()
                  .like(ShowcaseApp::getDeveloper, trimmed)
                  .or()
                  .like(ShowcaseApp::getOwner, trimmed)
                  .or()
                  .like(ShowcaseApp::getAppStatus, trimmed)
                  .or()
                  .like(ShowcaseApp::getVersion, trimmed)
                  .or()
                  .like(ShowcaseApp::getLicenseName, trimmed);
            });
    return wrapper;
  }
}
