package edu.jmi.openatom.server.openatomsystem.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.web.ApplicationSubmitRateLimitInterceptor;
import edu.jmi.openatom.server.openatomsystem.common.web.OperationLogInterceptor;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 *
 * <p>注册 Sa-Token 拦截器实现登录校验, 配置全局跨域过滤器, 并注册操作日志拦截器
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfigure implements WebMvcConfigurer {

	private final OperationLogInterceptor operationLogInterceptor;
	private final ApplicationSubmitRateLimitInterceptor applicationSubmitRateLimitInterceptor;

	@Value("${app.cors.allowed-origin-patterns:*}")
	private String[] allowedOriginPatterns;

	@Bean
	public StpLogic getStpLogicJwt() {
		return new StpLogicJwtForSimple();
	}

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(Arrays.asList(allowedOriginPatterns));
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "jmiopenatom", "X-Requested-With", "Accept", "Origin"));
		config.setExposedHeaders(Arrays.asList("Content-Disposition"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(operationLogInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns("/error", "/favicon.ico");

		registry.addInterceptor(applicationSubmitRateLimitInterceptor)
				.addPathPatterns("/applications");

		registry.addInterceptor(new SaInterceptor(handle -> {
			String path = SaHolder.getRequest().getRequestPath();
			String method = SaHolder.getRequest().getMethod();
			if ("/applications".equals(path) && "POST".equalsIgnoreCase(method)) {
				return;
			}
			StpUtil.checkLogin();
		})).addPathPatterns("/**").excludePathPatterns(
				"/auth/register",
				"/auth/login",
				"/auth/miniapp-login",
				"/auth/refresh-token",
				"/auth/introspect",
				"/auth/qq-bind",
				"/auth/qq-bind/confirm",
				"/auth/qq-bind/status",
				"/.well-known/openid-configuration",
				"/oauth/authorize",
				"/oauth/token",
				"/oauth/introspect",
				"/oauth/userinfo",
				"/oauth/jwks",
				"/bot/leave-applications",
				"/bot/leave-applications/**",
					"/bot/qq-events",
					"/bot/users/lookup",
					"/bot/evening-study/**",
					"/site/**",
				"/clubs/{clubId}/departments",
				"/files/avatars/**",
				"/files/images/**",
				"/public/**");
	}

	/**
	 * 注册 Sa-Token 全局过滤器，统一处理跨域
	 */
	@Bean
	public SaServletFilter getSaServletFilter() {
		return new SaServletFilter().addInclude("/**").setBeforeAuth(obj -> {
			String origin = SaHolder.getRequest().getHeader("Origin");
			String requestHeaders = SaHolder.getRequest().getHeader("Access-Control-Request-Headers");
			String allowHeaders = requestHeaders == null || requestHeaders.isBlank()
					? "Content-Type, Authorization, jmiopenatom, X-Requested-With, Accept, Origin"
					: requestHeaders;
			if (origin != null && !origin.isBlank()) {
				SaHolder.getResponse().setHeader("Access-Control-Allow-Origin", origin);
			}
			SaHolder.getResponse()
					.setHeader("Vary", "Origin, Access-Control-Request-Headers, Access-Control-Request-Method")
					.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
					.setHeader("Access-Control-Allow-Headers", allowHeaders)
					.setHeader("Access-Control-Expose-Headers", "Content-Disposition")
					.setHeader("Access-Control-Allow-Credentials", "true")
					.setHeader("Access-Control-Max-Age", "3600");

			// 2. 如果是 OPTIONS 预检请求，直接结束请求，不进入后面的拦截器
			SaRouter.match(SaHttpMethod.OPTIONS).free(res -> {
				SaRouter.back(); // 直接返回，不再往下走
			});
		}).setAuth(obj -> {
			// 这里可以写全局权限校验，或者留空
		});
	}
}
