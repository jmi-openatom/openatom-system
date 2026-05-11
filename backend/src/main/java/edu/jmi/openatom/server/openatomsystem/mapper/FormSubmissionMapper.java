package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.FormSubmission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表单提交数据访问层
 *
 * <p>提供对表单提交记录(FormSubmission)的数据库操作, 包括按表单ID查询提交记录以及清空提交用户ID等功能
 */
@Mapper
public interface FormSubmissionMapper extends BaseMapper<FormSubmission> {

  /** 按表单查记录（orderByDesc id） */
  default List<FormSubmission> selectByFormIdOrdered(Integer formId) {
    return selectList(
        new LambdaQueryWrapper<FormSubmission>()
            .eq(FormSubmission::getFormId, formId)
            .orderByDesc(FormSubmission::getId));
  }

  /** 清空 userId (用户被删除时) */
  default void nullifyUserId(Integer userId) {
    update(
        null,
        new LambdaUpdateWrapper<FormSubmission>()
            .eq(FormSubmission::getUserId, userId)
            .set(FormSubmission::getUserId, null));
  }
}
