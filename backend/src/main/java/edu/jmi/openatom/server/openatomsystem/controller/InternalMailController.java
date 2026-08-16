package edu.jmi.openatom.server.openatomsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal machine-to-machine surface for the mail system (protected by a
 * shared service token via {@code MailInternalAuthFilter}). Returns main-site
 * users that have an external email so the mail admin console can pick
 * recipients for broadcast emails.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/mail")
public class InternalMailController {
  private static final String MAIL_DOMAIN = "jmi-openatom.cn";

  private final UserMapper userMapper;

  @GetMapping("/users")
  public Result<PageDataVO<ExternalUserVO>> users(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "100") long pageSize,
      @RequestParam(required = false) String keyword) {
    long size = Math.min(500, Math.max(1, pageSize));
    Page<User> result =
        userMapper.selectPageByConditions(
            new Page<>(Math.max(1, page), size), keyword == null ? "" : keyword, null, null);
    List<ExternalUserVO> rows =
        result.getRecords().stream()
            .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
            .filter(user -> !user.getEmail().toLowerCase().endsWith("@" + MAIL_DOMAIN))
            .map(
                user ->
                    new ExternalUserVO(
                        user.getId(),
                        user.getRealName() != null && !user.getRealName().isBlank()
                            ? user.getRealName()
                            : user.getUserName(),
                        user.getEmail()))
            .toList();
    return Result.success(
        new PageDataVO<>(rows, result.getCurrent(), result.getSize(), result.getTotal()));
  }

  public record ExternalUserVO(long userId, String name, String email) {}
}