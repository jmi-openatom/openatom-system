package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ImageHostingAsset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图床资产数据访问层
 *
 * <p>提供前台个人图床和后台图床管理的分页查询
 */
@Mapper
public interface ImageHostingAssetMapper extends BaseMapper<ImageHostingAsset> {

  default Page<ImageHostingAsset> selectPageByConditions(
      Page<ImageHostingAsset> page,
      Integer uploaderId,
      String keyword,
      String status,
      boolean activeOnly) {
    LambdaQueryWrapper<ImageHostingAsset> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(uploaderId != null, ImageHostingAsset::getUploaderId, uploaderId)
        .eq(activeOnly, ImageHostingAsset::getStatus, "active")
        .eq(
            !activeOnly && status != null && !status.isBlank(),
            ImageHostingAsset::getStatus,
            status)
        .and(
            keyword != null && !keyword.isBlank(),
            query -> {
              String trimmed = keyword.trim();
              query
                  .like(ImageHostingAsset::getOriginalName, trimmed)
                  .or()
                  .like(ImageHostingAsset::getFileName, trimmed)
                  .or()
                  .like(ImageHostingAsset::getUrl, trimmed);
            })
        .orderByDesc(ImageHostingAsset::getId);
    return selectPage(page, wrapper);
  }

  default ImageHostingAsset selectActiveByFileName(String fileName) {
    return selectOne(
        new LambdaQueryWrapper<ImageHostingAsset>()
            .eq(ImageHostingAsset::getFileName, fileName)
            .eq(ImageHostingAsset::getStatus, "active")
            .last("LIMIT 1"));
  }

  default ImageHostingAsset selectByFileName(String fileName) {
    return selectOne(
        new LambdaQueryWrapper<ImageHostingAsset>()
            .eq(ImageHostingAsset::getFileName, fileName)
            .last("LIMIT 1"));
  }
}
