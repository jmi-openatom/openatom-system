package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 *
 * <p>提供对用户(User)的数据库操作, 包括按学号或用户名查重, 登录查询, 模糊搜索用户以及条件分页查询用户等功能
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

  /** 按学号或用户名统计数量（检查是否已存在） */
  default Long countByUsernameOrStudentId(String username, String studentId) {
    return selectCount(
        new LambdaQueryWrapper<User>()
            .eq(User::getUserName, username)
            .or()
            .eq(User::getStudentId, studentId));
  }

  /** 登录时按学号或用户名查用户 */
  default User selectByStudentIdOrUserName(String loginName) {
    return selectOne(
        new LambdaQueryWrapper<User>()
            .eq(User::getStudentId, loginName)
            .or()
            .eq(User::getUserName, loginName));
  }

  /** 模糊搜索用户（用户名/真实姓名/学号/手机/邮箱） */
  default List<User> searchByKeyword(String keyword) {
    return selectList(
        new LambdaQueryWrapper<User>()
            .like(User::getUserName, keyword)
            .or()
            .like(User::getRealName, keyword)
            .or()
            .like(User::getStudentId, keyword)
            .or()
            .like(User::getPhone, keyword)
            .or()
            .like(User::getEmail, keyword));
  }

  /** 模糊搜索用户（用户名/真实姓名/学号） */
  default List<User> searchByNameKeyword(String keyword) {
    return selectList(
        new LambdaQueryWrapper<User>()
            .like(User::getUserName, keyword)
            .or()
            .like(User::getRealName, keyword)
            .or()
            .like(User::getStudentId, keyword));
  }

  /** 模糊搜索用户选项（Word导出人员选择） */
  default List<User> selectOptionsByKeyword(String keyword) {
    LambdaQueryWrapper<User> wrapper =
        new LambdaQueryWrapper<User>()
            .select(
                User::getId,
                User::getRealName,
                User::getStudentId,
                User::getCollege,
                User::getMajor,
                User::getGrade,
                User::getClassName,
                User::getPhone)
            .orderByAsc(User::getStudentId)
            .orderByAsc(User::getId);
    if (keyword != null && !keyword.isBlank()) {
      String trimmed = keyword.trim();
      wrapper.and(
          query ->
              query
                  .like(User::getRealName, trimmed)
                  .or()
                  .like(User::getStudentId, trimmed)
                  .or()
                  .like(User::getMajor, trimmed)
                  .or()
                  .like(User::getClassName, trimmed));
    }
    return selectList(wrapper);
  }

  /** 条件查询用户分页 */
  default Page<User> selectPageByConditions(
      Page<User> page, String keyword, UserStatus status, List<Integer> userIds) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    if (keyword != null && !keyword.isBlank()) {
      String k = keyword.trim();
      wrapper.and(
          q ->
              q.like(User::getUserName, k)
                  .or()
                  .like(User::getRealName, k)
                  .or()
                  .like(User::getStudentId, k)
                  .or()
                  .like(User::getPhone, k)
                  .or()
                  .like(User::getEmail, k));
    }
    if (status != null) {
      wrapper.eq(User::getUserStatus, status);
    }
    if (userIds != null && !userIds.isEmpty()) {
      wrapper.in(User::getId, userIds);
    }
    wrapper.orderByDesc(User::getId);
    return selectPage(page, wrapper);
  }
}
