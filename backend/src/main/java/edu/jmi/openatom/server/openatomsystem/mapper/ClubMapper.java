package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClubMapper extends BaseMapper<Club> {

  /** 条件查询社团 */
  default List<Club> selectByConditions(
      String keyword, String category, String status, String recruitmentStatus) {
    LambdaQueryWrapper<Club> wrapper = new LambdaQueryWrapper<>();
    if (keyword != null && !keyword.isBlank()) {
      wrapper.and(q -> q.like(Club::getName, keyword).or().like(Club::getCode, keyword));
    }
    wrapper
        .eq(category != null && !category.isBlank(), Club::getCategory, category)
        .eq(status != null && !status.isBlank(), Club::getStatus, status)
        .eq(
            recruitmentStatus != null && !recruitmentStatus.isBlank(),
            Club::getRecruitmentStatus,
            recruitmentStatus);
    return selectList(wrapper);
  }

  /** 按编码查社团（用于唯一性检查） */
  default Long countByCode(String code) {
    return selectCount(new LambdaQueryWrapper<Club>().eq(Club::getCode, code));
  }

  /** 查默认社团 */
  default Club selectDefaultClub(String defaultCode) {
    return selectOne(new LambdaQueryWrapper<Club>().eq(Club::getCode, defaultCode));
  }

  /** 清空负责人 (用户被删除时) */
  default void nullifyPresidentUserId(Integer userId) {
    update(
        null,
        new LambdaUpdateWrapper<Club>()
            .eq(Club::getPresidentUserId, userId)
            .set(Club::getPresidentUserId, null));
  }
  /** 查所有 active 状态的社团 */
  default List<Club> selectActiveClubs() {
    return selectList(new LambdaQueryWrapper<Club>().eq(Club::getStatus, "active"));
  }
}
