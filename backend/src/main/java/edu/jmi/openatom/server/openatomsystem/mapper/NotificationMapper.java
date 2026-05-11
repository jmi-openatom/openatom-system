package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知数据访问层
 *
 * <p>提供对通知(Notification)的数据库操作, 包括查询所有通知并按创建时间降序排序等功能
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

  /** 查所有通知（ordered by createdAt desc） */
  default List<Notification> selectAllOrdered() {
    return selectList(
        new LambdaQueryWrapper<Notification>().orderByDesc(Notification::getCreatedAt));
  }
}
