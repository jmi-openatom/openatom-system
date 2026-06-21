package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.VoteCampaign;
import edu.jmi.openatom.server.openatomsystem.entity.VoteOption;
import edu.jmi.openatom.server.openatomsystem.entity.VoteRecord;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.VoteCampaignMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.VoteOptionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.VoteRecordMapper;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteDetailVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

  @Mock private VoteCampaignMapper voteCampaignMapper;
  @Mock private VoteOptionMapper voteOptionMapper;
  @Mock private VoteRecordMapper voteRecordMapper;
  @Mock private ClubMapper clubMapper;
  @Mock private UserMapper userMapper;

  private VoteServiceImpl voteService;

  @BeforeEach
  void setUp() {
    voteService =
        new VoteServiceImpl(
            voteCampaignMapper, voteOptionMapper, voteRecordMapper, clubMapper, userMapper);
  }

  @Test
  void privateResultsStayHiddenOnSiteButRemainVisibleInAdmin() {
    VoteCampaign campaign =
        VoteCampaign.builder()
            .id(1)
            .clubId(1)
            .title("内部投票")
            .status("closed")
            .voteType("single")
            .maxChoices(1)
            .anonymousAllowed(true)
            .resultVisible(false)
            .resultVisibility("private")
            .build();
    VoteOption option =
        VoteOption.builder().id(11).voteId(1).title("选项 A").sortOrder(10).build();
    VoteRecord record =
        VoteRecord.builder()
            .id(21)
            .voteId(1)
            .voterKey("anon:test@example.com")
            .selectedOptionIds("[\"11\"]")
            .build();

    when(voteCampaignMapper.selectById(1)).thenReturn(campaign);
    when(voteOptionMapper.selectByVoteId(1)).thenReturn(List.of(option));
    when(voteRecordMapper.selectByVoteId(1)).thenReturn(List.of(record));
    when(clubMapper.selectById(1)).thenReturn(Club.builder().id(1).name("测试社团").build());

    try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
      stpUtil.when(StpUtil::isLogin).thenReturn(false);
      ResponseVoteDetailVO siteDetail = voteService.siteDetail(1).getData();
      assertEquals("private", siteDetail.getVote().getResultVisibility());
      assertNull(siteDetail.getVote().getVoterCount());
      assertNull(siteDetail.getVote().getTotalVoteCount());
      assertNull(siteDetail.getOptions().getFirst().getVoteCount());
      assertNull(siteDetail.getOptions().getFirst().getVotePercent());
      assertEquals(List.of(), siteDetail.getRecords());

      ResponseVoteDetailVO adminDetail = voteService.detail(1).getData();
      assertEquals(1, adminDetail.getVote().getVoterCount());
      assertEquals(1, adminDetail.getOptions().getFirst().getVoteCount());
      assertFalse(adminDetail.getRecords().isEmpty());
    }
  }
}
