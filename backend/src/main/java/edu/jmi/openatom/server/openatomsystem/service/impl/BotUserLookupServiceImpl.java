package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.BotUserLookupService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBotUserLookupVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 机器人公开查人实现。 */
@Service
@RequiredArgsConstructor
public class BotUserLookupServiceImpl implements BotUserLookupService {
  private static final int DEFAULT_LIMIT = 5;
  private static final int MAX_LIMIT = 10;

  private final UserMapper userMapper;

  @Override
  public Result<List<ResponseBotUserLookupVO>> lookup(String keyword, String qqOpenid, Integer limit) {
    String normalizedKeyword = trimToNull(keyword);
    String normalizedQqOpenid = trimToNull(qqOpenid);
    if (normalizedKeyword == null && normalizedQqOpenid == null) {
      return Result.error(400, "请提供姓名或QQ号");
    }
    return Result.success(
        userMapper
            .selectBotLookup(normalizedKeyword, normalizedQqOpenid, normalizeLimit(limit))
            .stream()
            .map(this::toResponse)
            .toList());
  }

  private ResponseBotUserLookupVO toResponse(User user) {
    return ResponseBotUserLookupVO.builder()
        .id(user.getId())
        .userName(user.getUserName())
        .realName(user.getRealName())
        .studentId(user.getStudentId())
        .college(user.getCollege())
        .major(user.getMajor())
        .grade(user.getGrade())
        .className(user.getClassName())
        .qqBound(trimToNull(user.getQqOpenid()) != null)
        .userStatus(user.getUserStatus() == null ? null : user.getUserStatus().jsonValue())
        .build();
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.max(1, Math.min(limit, MAX_LIMIT));
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
