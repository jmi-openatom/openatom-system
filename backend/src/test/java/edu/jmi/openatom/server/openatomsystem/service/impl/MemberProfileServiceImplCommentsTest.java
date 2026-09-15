package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileComment;
import edu.jmi.openatom.server.openatomsystem.mapper.CommentReportMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MemberProfileCommentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MemberProfileMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 后台主页评论列表的回归测试。
 *
 * <p>修复了两类会返回 500 的缺陷：
 *
 * <ul>
 *   <li>MyBatis-Plus 的 selectBatchIds 不接受空集合，空集合会生成非法的 {@code WHERE id IN ()}；
 *       当当前页只有一级评论（parentId 为 null）时就会出现这种情况。
 *   <li>{@code Map.of()} 是空映射时，{@code get(null)} 会抛出 NPE。
 * </ul>
 *
 * <p>历史修复（handle root comments without parents）只覆盖了公开评论列表，后台列表被遗漏。
 */
class MemberProfileServiceImplCommentsTest {

  @Test
  void adminCommentsDoesNotQueryBatchIdsWhenAllCommentsAreRoots() {
    MemberProfileComment root =
        MemberProfileComment.builder()
            .id(1L)
            .profileUserId(10)
            .userId(20)
            .parentId(null)
            .content("一级评论")
            .status("visible")
            .build();
    Page<MemberProfileComment> page = new Page<>(1, 10);
    page.setRecords(List.of(root));
    page.setTotal(1);

    MemberProfileCommentMapper commentMapper = mock(MemberProfileCommentMapper.class);
    when(commentMapper.selectAdminPage(any(), any(), any())).thenReturn(page);
    when(commentMapper.selectBatchIds(anyCollection()))
        .thenAnswer(
            invocation -> {
              Collection<?> ids = invocation.getArgument(0);
              if (ids.isEmpty()) {
                throw new AssertionError("selectBatchIds must not be called with an empty id list");
              }
              return List.of();
            });

    CommentReportMapper reportMapper = mock(CommentReportMapper.class);
    when(reportMapper.selectPendingByComments(any(), anyCollection())).thenReturn(List.of());

    MemberProfileMapper profileMapper = mock(MemberProfileMapper.class);
    when(profileMapper.selectByUserIds(anyList())).thenReturn(List.of());

    UserMapper userMapper = mock(UserMapper.class);
    when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

    MemberProfileServiceImpl service =
        new MemberProfileServiceImpl(
            profileMapper,
            null,
            commentMapper,
            null,
            reportMapper,
            null,
            null,
            userMapper,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    var result = service.adminComments(null, null, 1L, 10L);

    assertEquals(0, result.getCode());
    assertEquals(1, result.getData().getList().size());
    assertEquals(1L, result.getData().getList().getFirst().getId());
  }
}
