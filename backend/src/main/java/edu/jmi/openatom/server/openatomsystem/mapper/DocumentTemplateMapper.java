package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.DocumentTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentTemplateMapper extends BaseMapper<DocumentTemplate> {
  default List<DocumentTemplate> selectByConditions(String templateType, String status) {
    LambdaQueryWrapper<DocumentTemplate> wrapper =
        new LambdaQueryWrapper<DocumentTemplate>().orderByDesc(DocumentTemplate::getId);
    if (templateType != null && !templateType.isBlank()) {
      wrapper.eq(DocumentTemplate::getTemplateType, templateType);
    }
    if (status != null && !status.isBlank()) {
      wrapper.eq(DocumentTemplate::getStatus, status);
    }
    return selectList(wrapper);
  }

  default DocumentTemplate selectLatestEnabledByType(String templateType) {
    return selectOne(
        new LambdaQueryWrapper<DocumentTemplate>()
            .eq(DocumentTemplate::getTemplateType, templateType)
            .eq(DocumentTemplate::getStatus, "enabled")
            .orderByDesc(DocumentTemplate::getVersion)
            .orderByDesc(DocumentTemplate::getId)
            .last("LIMIT 1"));
  }
}
