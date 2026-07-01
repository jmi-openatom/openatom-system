package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.NextPageStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * Next 页面统计数据访问层
 *
 * <p>提供访问量递增和点赞数递增操作
 */
@Mapper
public interface NextPageStatsMapper extends BaseMapper<NextPageStats> {

  default int incrementViewCount() {
    return update(
        null,
        new LambdaUpdateWrapper<NextPageStats>()
            .eq(NextPageStats::getId, 1)
            .setSql("view_count = COALESCE(view_count, 0) + 1"));
  }

  default int incrementLikeCount() {
    return update(
        null,
        new LambdaUpdateWrapper<NextPageStats>()
            .eq(NextPageStats::getId, 1)
            .setSql("like_count = COALESCE(like_count, 0) + 1"));
  }
}
