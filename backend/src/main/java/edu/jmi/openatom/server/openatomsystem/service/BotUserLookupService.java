package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBotUserLookupVO;
import java.util.List;

/** 机器人公开查人服务。 */
public interface BotUserLookupService {
  Result<List<ResponseBotUserLookupVO>> lookup(String keyword, String qqOpenid, Integer limit);
}
