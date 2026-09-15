package edu.jmi.openatom.quest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({OauthProperties.class, QuestProperties.class})
public class QuestConfiguration {

    @Bean
    RestClient oauthRestClient(OauthProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.issuer().replaceAll("/+$", "")).build();
    }
}
