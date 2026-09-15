package edu.jmi.openatom.quest.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AccessMapper {

    @Select("SELECT id FROM quest_role WHERE role_key = #{roleKey} LIMIT 1")
    Long findRoleId(@Param("roleKey") String roleKey);

    @Insert("INSERT IGNORE INTO quest_member_role (member_id, role_id, assigned_at) VALUES (#{memberId}, #{roleId}, NOW(3))")
    int assignRole(@Param("memberId") Long memberId, @Param("roleId") Long roleId);

    @Insert("INSERT IGNORE INTO quest_onboarding_progress (member_id, current_step, completed_steps_json) VALUES (#{memberId}, 1, JSON_ARRAY())")
    int createOnboarding(@Param("memberId") Long memberId);

    @Select("SELECT r.role_key FROM quest_member_role mr JOIN quest_role r ON r.id = mr.role_id WHERE mr.member_id = #{memberId} ORDER BY r.id")
    List<String> findRoleKeys(@Param("memberId") Long memberId);

    @Select("SELECT DISTINCT p.permission_key FROM quest_member_role mr JOIN quest_role_permission rp ON rp.role_id = mr.role_id JOIN quest_permission p ON p.id = rp.permission_id WHERE mr.member_id = #{memberId} ORDER BY p.permission_key")
    List<String> findPermissionKeys(@Param("memberId") Long memberId);

    @Delete("DELETE FROM quest_member_direction WHERE member_id = #{memberId}")
    int deleteMemberDirections(@Param("memberId") Long memberId);

    @Insert("INSERT INTO quest_member_direction (member_id, direction_id, is_primary) VALUES (#{memberId}, #{directionId}, #{primary})")
    int addMemberDirection(
        @Param("memberId") Long memberId,
        @Param("directionId") Long directionId,
        @Param("primary") boolean primary
    );
}
