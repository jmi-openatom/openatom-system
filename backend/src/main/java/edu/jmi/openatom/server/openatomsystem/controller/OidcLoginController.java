package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.security.OAuthSessionCookieService;
import edu.jmi.openatom.server.openatomsystem.service.AuthService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/** Central, same-origin login surface for OAuth authorization requests. */
@Controller
@RequiredArgsConstructor
public class OidcLoginController {
  private static final String LOGIN_CSP =
      "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; "
          + "base-uri 'none'; form-action 'self'; frame-ancestors 'none'";

  private final AuthService authService;
  private final OAuthSessionCookieService cookieService;

  @GetMapping(value = "/oauth/login", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<Resource> loginPage() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Content-Security-Policy", LOGIN_CSP)
        .header("X-Frame-Options", "DENY")
        .header("Referrer-Policy", "no-referrer")
        .body(new ClassPathResource("static/oauth-login.html"));
  }

  @PostMapping(value = "/oauth/session/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Result<Map<String, String>> login(
      @Valid @RequestBody RequestLoginDTO requestLoginDTO,
      HttpServletRequest request,
      HttpServletResponse response) {
    Result<ResponseLoginVO> result = authService.login(requestLoginDTO);
    if (result.getCode() != Result.SUCCESS_CODE || result.getData() == null) {
      return Result.error(result.getCode(), result.getMessage());
    }
    ResponseLoginVO login = result.getData();
    cookieService.write(request, response, login.getAccessToken(), login.getExpiresIn());
    return Result.success(Map.of("status", "authenticated"), "登录成功");
  }
}
