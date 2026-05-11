package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.OfficeDocument;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 办公文档数据访问层
 *
 * <p>提供对办公文档(OfficeDocument)的数据库操作, 包括按社团和类型关键字条件查询文档等功能
 */
@Mapper
public interface OfficeDocumentMapper extends BaseMapper<OfficeDocument> {

  /** 按社团和类型查单据（ordered by id desc） */
  default List<OfficeDocument> selectByConditions(Integer clubId, String docType, String keyword) {
    LambdaQueryWrapper<OfficeDocument> wrapper =
        new LambdaQueryWrapper<OfficeDocument>()
            .eq(OfficeDocument::getClubId, clubId)
            .orderByDesc(OfficeDocument::getId);
    if (docType != null && !docType.isBlank()) {
      wrapper.eq(OfficeDocument::getDocType, docType);
    }
    if (keyword != null && !keyword.isBlank()) {
      wrapper.like(OfficeDocument::getTitle, keyword.trim());
    }
    return selectList(wrapper);
  }
}
