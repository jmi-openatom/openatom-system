package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;

import java.util.Map;

public interface IApplicationSubmissionRedisService {
	void cacheSubmission(MembershipApplication application, Map<String, Object> profile);
}
