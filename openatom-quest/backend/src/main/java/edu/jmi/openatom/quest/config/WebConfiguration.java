package edu.jmi.openatom.quest.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {
    private final MemberAccessInterceptor memberAccessInterceptor;
    private final QuestProperties questProperties;

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/api/site-exploration/flags/**")
            .allowedOrigins(questProperties.siteUrl())
            .allowedMethods("GET")
            .allowCredentials(true)
            .maxAge(1800);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(memberAccessInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/system/health",
                "/api/auth/login",
                "/api/auth/callback",
                "/api/auth/session",
                "/api/auth/logout",
                "/api/dev/**"
            );
    }
}
