package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.NextPageStats;
import edu.jmi.openatom.server.openatomsystem.mapper.NextPageStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Next 页面统计服务
 *
 * <p>提供页面统计数据查询、访问量和点赞数递增操作
 */
@Service
@RequiredArgsConstructor
public class NextPageStatsService {

  private final NextPageStatsMapper nextPageStatsMapper;

  /**
   * 获取页面统计数据并递增访问量
   */
  public Result<Map<String, Object>> getStatsAndIncrementView() {
    // 递增访问量
    nextPageStatsMapper.incrementViewCount();

    // 查询最新数据
    NextPageStats stats = nextPageStatsMapper.selectById(1);
    if (stats == null) {
      return Result.error("统计数据不存在");
    }

    return Result.success(Map.of(
        "viewCount", stats.getViewCount(),
        "likeCount", stats.getLikeCount()
    ));
  }

  /**
   * 递增点赞数
   */
  public Result<Map<String, Object>> incrementLike() {
    nextPageStatsMapper.incrementLikeCount();

    // 查询最新数据
    NextPageStats stats = nextPageStatsMapper.selectById(1);
    if (stats == null) {
      return Result.error("统计数据不存在");
    }

    return Result.success(Map.of(
        "viewCount", stats.getViewCount(),
        "likeCount", stats.getLikeCount()
    ), "点赞成功");
  }
}
