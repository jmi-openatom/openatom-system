package edu.jmi.openatom.quest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quest")
public record QuestProperties(String frontendUrl) {
}
