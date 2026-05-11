package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.NotificationReceiver;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知接收人数据访问层
 *
 * <p>提供对通知接收人(NotificationReceiver)的数据库操作, 包括按接收人查询通知, 按通知ID和用户ID查询更新以及统计未读数等功能
 */
@Mapper
public interface NotificationReceiverMapper extends BaseMapper<NotificationReceiver> {

  /** 按接收人查 */
  default List<NotificationReceiver> selectByReceiverUserId(Integer userId) {
    return selectList(
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getReceiverUserId, userId));
  }

  /** 查单条（按通知ID和用户ID） */
  default NotificationReceiver selectOneByNotificationAndUser(
      Integer notificationId, Integer userId) {
    return selectOne(
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getNotificationId, notificationId)
            .eq(NotificationReceiver::getReceiverUserId, userId));
  }

  /** 按通知ID和用户ID更新 */
  default int updateByNotificationAndUser(
      NotificationReceiver entity, Integer notificationId, Integer userId) {
    return update(
        entity,
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getNotificationId, notificationId)
            .eq(NotificationReceiver::getReceiverUserId, userId));
  }

  /** 统计未读数 */
  default Long countUnread(Integer userId) {
    return selectCount(
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getReceiverUserId, userId)
            .eq(NotificationReceiver::getReadFlag, 0));
  }

  /** 按通知ID删除 */
  default int deleteByNotificationId(Integer notificationId) {
    return delete(
        new LambdaQueryWrapper<NotificationReceiver>()
            .eq(NotificationReceiver::getNotificationId, notificationId));
  }
}
