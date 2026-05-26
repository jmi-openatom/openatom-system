package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestDrawLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryScreenVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryWinnerVO;
import java.util.List;

/** 抽奖系统业务接口 */
public interface LotteryService {
  Result<List<ResponseLotteryVO>> list(Integer clubId, String status);

  Result<ResponseLotteryDetailVO> detail(Integer lotteryId);

  Result<String> create(Integer clubId, RequestCreateLotteryDTO request);

  Result<String> update(Integer lotteryId, RequestUpdateLotteryDTO request);

  Result<String> publish(Integer lotteryId);

  Result<String> close(Integer lotteryId);

  Result<ResponseLotteryWinnerVO> draw(Integer lotteryId, RequestDrawLotteryDTO request);

  Result<String> reset(Integer lotteryId);

  Result<ResponseLotteryScreenVO> screen(Integer lotteryId);
}
