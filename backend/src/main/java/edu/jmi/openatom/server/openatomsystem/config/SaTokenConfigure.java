package edu.jmi.openatom.server.openatomsystem.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.web.ApplicationSubmitRateLimitInterceptor;
import edu.jmi.openatom.server.openatomsystem.common.web.OperationLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(operationLogInterceptor).addPathPatterns("/**").excludePathPatterns("/error", "/favicon.ico");

		registry.addInterceptor(applicationSubmitRateLimitInterceptor).addPathPatterns("/applications");

		registry.addInterceptor(new SaInterceptor(handle -> {
			String path = SaHolder.getRequest().getRequestPath();
			String method = SaHolder.getRequest().getMethod();
			if ("/applications".equals(path) && "POST".equalsIgnoreCase(method)) {
				return;
			}
			StpUtil.checkLogin();
		})).addPathPatterns("/**").excludePathPatterns("/auth/register", "/auth/login", "/auth/miniapp-login", "/auth/refresh-token", "/auth/qq-bind", "/auth/qq-bind/confirm", "/auth/qq-bind/status", "/bot/leave-applications", "/bot/leave-applications/**", "/bot/qq-events", "/bot/users/lookup", "/site/**", "/clubs/{clubId}/departments", "/files/avatars/**", "/files/images/**");
	}

	/**
	 * 注册 Sa-Token 全局过滤器，统一处理跨域1
	 */
	@Bean
	public SaServletFilter getSaServletFilter() {
		return new SaServletFilter().addInclude("/**").setBeforeAuth(obj -> {
			// 1. 设置跨域响应头
			// 注意：如果 allowCredentials 为 true，origin 不能为 *，必须是具体域名
			// 这里我们直接从配置读取，或者如果你想完全放开，可以直接设为 SaHolder.getRequest().getHeader("Origin")
			SaHolder.getResponse().setHeader("Access-Control-Allow-Origin", SaHolder.getRequest().getHeader("Origin")).setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS").setHeader("Access-Control-Allow-Headers", "*").setHeader("Access-Control-Expose-Headers", "Content-Disposition").setHeader("Access-Control-Allow-Credentials", "true") // 建议设为 true 以支持 Token
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
