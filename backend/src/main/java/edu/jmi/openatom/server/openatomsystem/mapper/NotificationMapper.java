package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Notification;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

  /** 查所有通知（ordered by createdAt desc） */
  default List<Notification> selectAllOrdered() {
    return selectList(
        new LambdaQueryWrapper<Notification>().orderByDesc(Notification::getCreatedAt));
  }
}
