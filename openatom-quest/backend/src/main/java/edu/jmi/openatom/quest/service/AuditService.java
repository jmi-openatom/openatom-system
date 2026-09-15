package edu.jmi.openatom.quest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void record(Long actorId, String action, String targetType, Object targetId, Map<String, ?> detail) {
        jdbcTemplate.update(
            "INSERT INTO quest_audit_log (actor_member_id, action, target_type, target_id, result, detail_json) VALUES (?, ?, ?, ?, 'SUCCESS', ?)",
            actorId,
            action,
            targetType,
            targetId == null ? null : String.valueOf(targetId),
            json(detail)
        );
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("操作日志序列化失败", exception);
        }
    }
}
