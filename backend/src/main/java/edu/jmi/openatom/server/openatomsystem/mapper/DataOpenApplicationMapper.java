package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.DataOpenApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据开放平台申请数据访问层
 *
 * <p>提供公开申请、后台审核列表和密钥查询
 */
@Mapper
public interface DataOpenApplicationMapper extends BaseMapper<DataOpenApplication> {

  default Page<DataOpenApplication> selectPageByConditions(
      Page<DataOpenApplication> page, String keyword, String status) {
    LambdaQueryWrapper<DataOpenApplication> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(status != null && !status.isBlank(), DataOpenApplication::getStatus, status)
        .and(
            keyword != null && !keyword.isBlank(),
            query -> {
              String trimmed = keyword.trim();
              query
                  .like(DataOpenApplication::getAppName, trimmed)
                  .or()
                  .like(DataOpenApplication::getApplicantName, trimmed)
                  .or()
                  .like(DataOpenApplication::getApplicantContact, trimmed)
                  .or()
                  .like(DataOpenApplication::getOrganization, trimmed);
            })
        .orderByDesc(DataOpenApplication::getId);
    return selectPage(page, wrapper);
  }

  default DataOpenApplication selectByApiKey(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) return null;
    return selectOne(
        new LambdaQueryWrapper<DataOpenApplication>()
            .eq(DataOpenApplication::getApiKey, apiKey.trim())
            .last("LIMIT 1"));
  }
}
