package edu.jmi.openatom.server.openatomsystem.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.web.OperationLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SaTokenConfigure implements WebMvcConfigurer {
  private final OperationLogInterceptor operationLogInterceptor;

  @Value("${app.cors.allowed-origin-patterns:*}")
  private String[] allowedOriginPatterns;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(operationLogInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/error", "/favicon.ico");

    registry
        .addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/auth/register", "/auth/login", "/auth/refresh-token", "/site/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns(allowedOriginPatterns)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Content-Disposition")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
