package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.service.IApplicationSubmissionRedisService;
import edu.jmi.openatom.server.openatomsystem.common.web.ClientIpResolver;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 将入会申请提交后的关键快照写入 Redis。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationSubmissionRedisServiceImpl implements IApplicationSubmissionRedisService {
	private static final String SNAPSHOT_KEY_PREFIX = "openatom:applications:submission:";
	private static final String RECENT_LIST_KEY = "openatom:applications:recent";
	private static final Duration SNAPSHOT_TTL = Duration.ofDays(30);
	private static final long RECENT_LIST_SIZE = 500L;

	private final StringRedisTemplate stringRedisTemplate;
	private final ClientIpResolver clientIpResolver;
	private final HttpServletRequest request;

	public void cacheSubmission(MembershipApplication application, Map<String, Object> profile) {
		if (application == null || application.getId() == null) {
			return;
		}

		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("applicationId", application.getId());
		snapshot.put("campaignId", application.getCampaignId());
		snapshot.put("clubId", application.getClubId());
		snapshot.put("userId", application.getUserId());
		snapshot.put("firstChoiceDepartmentId", application.getFirstChoiceDepartmentId());
		snapshot.put("secondChoiceDepartmentId", application.getSecondChoiceDepartmentId());
		snapshot.put("status", application.getStatus());
		snapshot.put("profile", profile == null ? Map.of() : profile);
		snapshot.put("clientIp", clientIpResolver.resolve(request));
		snapshot.put("submittedAt", Instant.now().toString());

		try {
			String key = SNAPSHOT_KEY_PREFIX + application.getId();
			stringRedisTemplate.opsForValue().set(key, Jsons.stringify(snapshot), SNAPSHOT_TTL);
			stringRedisTemplate.opsForList().leftPush(RECENT_LIST_KEY, String.valueOf(application.getId()));
			stringRedisTemplate.opsForList().trim(RECENT_LIST_KEY, 0, RECENT_LIST_SIZE - 1);
		} catch (RuntimeException e) {
			log.warn("Failed to cache membership application submission {}", application.getId(), e);
		}
	}
}
