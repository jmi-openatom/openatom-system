package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** 财务数据权限独立于菜单权限，按当前有效社团关系校验。 */
@Component
@RequiredArgsConstructor
public class ClubExpenseAccess {
  private final NamedParameterJdbcTemplate jdbc;

  public int actor() {
    return StpUtil.getLoginIdAsInt();
  }

  /** null 表示系统管理员可查看所有社团。 */
  public List<Integer> allowedClubIds() {
    if (StpUtil.hasRole("super_admin")) return null;
    return jdbc.query("""
        SELECT DISTINCT club_id FROM club_membership
        WHERE user_id = :userId AND left_at IS NULL AND status IN ('active', 'probation')
        """, new MapSqlParameterSource("userId", actor()), (rs, n) -> rs.getInt(1));
  }

  public void checkClub(Integer clubId) {
    if (clubId == null || clubId <= 0) throw new ClubExpenseException(400, "请选择所属社团");
    List<Integer> scope = allowedClubIds();
    if (scope != null && !scope.contains(clubId)) {
      throw new ClubExpenseException(403, "无权访问该社团的支出");
    }
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM club WHERE id = :clubId",
        new MapSqlParameterSource("clubId", clubId), Integer.class);
    if (count == null || count == 0) throw new ClubExpenseException(404, "社团不存在");
  }
}
