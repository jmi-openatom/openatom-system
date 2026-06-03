package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateVoteDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSubmitVoteDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateVoteDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteVO;
import java.util.List;

/** 投票服务 */
public interface VoteService {
  Result<List<ResponseVoteVO>> list(Integer clubId, String status);

  Result<ResponseVoteDetailVO> detail(Integer voteId);

  Result<String> create(Integer clubId, RequestCreateVoteDTO request);

  Result<String> update(Integer voteId, RequestUpdateVoteDTO request);

  Result<String> publish(Integer voteId);

  Result<String> close(Integer voteId);

  Result<String> reset(Integer voteId);

  Result<List<ResponseVoteVO>> siteList(Integer clubId);

  Result<ResponseVoteDetailVO> siteDetail(Integer voteId);

  Result<ResponseVoteRecordVO> submit(Integer voteId, RequestSubmitVoteDTO request);
}
