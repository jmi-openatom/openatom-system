package edu.jmi.openatom.server.openatomsystem.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.jmi.openatom.server.openatomsystem.common.web.ClientIpResolver;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 控制器日志切面
 *
 * <p>使用 AOP 对 Controller 层方法进行环绕增强, 记录请求参数, 响应结果, 执行耗时和异常信息, 并对敏感字段进行脱敏处理.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerLogAspect {
  private static final String MASKED_VALUE = "******";

  private final ObjectMapper objectMapper;
  private final ClientIpResolver clientIpResolver;

  @Around("within(edu.jmi.openatom.server.openatomsystem.controller..*)")
  public Object logControllerCall(ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    HttpServletRequest request = currentRequest();
    String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

    try {
      log.info(
          "Request start: method={}, uri={}, clientIp={}, args={}",
          methodName,
          request == null ? "N/A" : request.getMethod() + " " + request.getRequestURI(),
          request == null ? "N/A" : clientIpResolver.resolve(request),
          buildArgs(signature.getParameterNames(), joinPoint.getArgs()));

      Object result = joinPoint.proceed();

      log.info(
          "Request end: method={}, durationMs={}, result={}",
          methodName,
          System.currentTimeMillis() - startTime,
          summarizeResult(result));
      return result;
    } catch (Throwable throwable) {
      log.error(
          "Request error: method={}, durationMs={}, exception={}: {}",
          methodName,
          System.currentTimeMillis() - startTime,
          throwable.getClass().getSimpleName(),
          throwable.getMessage(),
          throwable);
      throw throwable;
    }
  }

  private HttpServletRequest currentRequest() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      return attrs.getRequest();
    }
    return null;
  }

  private Map<String, Object> buildArgs(String[] parameterNames, Object[] args) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      if (shouldSkipArg(arg)) {
        continue;
      }
      String name =
          parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
      result.put(name, maskSensitiveFields(toJsonNode(arg)));
    }
    return result;
  }

  private boolean shouldSkipArg(Object arg) {
    return arg instanceof ServletRequest
        || arg instanceof ServletResponse
        || arg instanceof BindingResult
        || arg instanceof MultipartFile;
  }

  private JsonNode toJsonNode(Object value) {
    return objectMapper.valueToTree(value);
  }

  private JsonNode maskSensitiveFields(JsonNode node) {
    if (node == null || node.isNull() || node.isValueNode()) {
      return node;
    }
    if (node.isArray()) {
      ArrayNode arrayNode = objectMapper.createArrayNode();
      node.forEach(item -> arrayNode.add(maskSensitiveFields(item)));
      return arrayNode;
    }

    ObjectNode objectNode = objectMapper.createObjectNode();
    node.fields()
        .forEachRemaining(
            entry -> {
              if (isSensitiveField(entry.getKey())) {
                objectNode.put(entry.getKey(), MASKED_VALUE);
              } else {
                objectNode.set(entry.getKey(), maskSensitiveFields(entry.getValue()));
              }
            });
    return objectNode;
  }

  private boolean isSensitiveField(String fieldName) {
    String normalized = fieldName.toLowerCase(Locale.ROOT);
    return normalized.contains("password")
        || normalized.contains("token")
        || normalized.contains("secret")
        || normalized.contains("credential");
  }

  private Object summarizeResult(Object result) {
    if (result == null) {
      return null;
    }
    if (result instanceof ResponseEntity<?> responseEntity) {
      Object body = responseEntity.getBody();
      Map<String, Object> summary = new LinkedHashMap<>();
      summary.put("status", responseEntity.getStatusCode().value());
      summary.put("bodyType", body == null ? null : body.getClass().getSimpleName());
      return summary;
    }
    if (result instanceof Resource resource) {
      return Map.of("resource", resource.getDescription());
    }
    return maskSensitiveFields(toJsonNode(result));
  }
}
