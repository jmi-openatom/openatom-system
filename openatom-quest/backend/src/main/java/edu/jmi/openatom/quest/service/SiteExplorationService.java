package edu.jmi.openatom.quest.service;

import edu.jmi.openatom.quest.dto.SubmitSiteExplorationRequest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteExplorationService {
    public static final String TASK_KEY = "site-exploration-l0";
    private static final Map<String, String> PAGE_LABELS = Map.of(
        "about", "关于我们",
        "regulations", "规章制度",
        "activities", "社团活动"
    );

    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    public String flagFor(long memberId, String pageKey) {
        if (!PAGE_LABELS.containsKey(pageKey)) {
            throw new IllegalArgumentException("探索页面不存在");
        }
        String existing = findFlag(memberId, pageKey);
        if (existing != null) return existing;

        for (int attempt = 0; attempt < 3; attempt++) {
            byte[] bytes = new byte[12];
            random.nextBytes(bytes);
            String flag = "OA{" + pageKey.toUpperCase(Locale.ROOT) + "-" + HexFormat.of().withUpperCase().formatHex(bytes) + "}";
            try {
                jdbcTemplate.update(
                    "INSERT INTO quest_site_exploration_flag (member_id, page_key, flag_value) VALUES (?, ?, ?)",
                    memberId, pageKey, flag
                );
                return flag;
            } catch (DuplicateKeyException exception) {
                existing = findFlag(memberId, pageKey);
                if (existing != null) return existing;
            }
        }
        throw new IllegalStateException("探索标记生成失败，请重试");
    }

    public void verify(long memberId, SubmitSiteExplorationRequest request) {
        Map<String, String> submitted = Map.of(
            "about", request.aboutFlag().trim(),
            "regulations", request.regulationsFlag().trim(),
            "activities", request.activitiesFlag().trim()
        );
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT page_key, flag_value FROM quest_site_exploration_flag WHERE member_id = ?", memberId);
        Map<String, String> expected = rows.stream().collect(java.util.stream.Collectors.toMap(
            row -> (String) row.get("page_key"), row -> (String) row.get("flag_value")));
        for (String pageKey : List.of("about", "regulations", "activities")) {
            String value = expected.get(pageKey);
            if (value == null || !value.equalsIgnoreCase(submitted.get(pageKey))) {
                throw new IllegalArgumentException(PAGE_LABELS.get(pageKey) + "的个人标记不正确，请回到该页面查看");
            }
        }
        if (request.reflection().trim().codePointCount(0, request.reflection().trim().length()) < 8) {
            throw new IllegalArgumentException("请用至少 8 个字写下你的参与计划");
        }
    }

    private String findFlag(long memberId, String pageKey) {
        List<String> flags = jdbcTemplate.queryForList(
            "SELECT flag_value FROM quest_site_exploration_flag WHERE member_id = ? AND page_key = ?",
            String.class, memberId, pageKey);
        return flags.isEmpty() ? null : flags.getFirst();
    }
}
