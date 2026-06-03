package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveShowcaseAppDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ShowcaseApp;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.util.List;

/**
 * 应用展示服务接口
 *
 * <p>定义应用展示的公开浏览和后台管理操作
 */
public interface ShowcaseAppService {
  Result<List<ShowcaseApp>> publicList(String keyword, Boolean openSource);

  Result<ShowcaseApp> publicDetail(Integer appId);

  Result<PageDataVO<ShowcaseApp>> adminList(
      String keyword, String status, Boolean openSource, Long page, Long pageSize);

  Result<ShowcaseApp> create(RequestSaveShowcaseAppDTO request, Integer operatorId);

  Result<ShowcaseApp> update(Integer appId, RequestSaveShowcaseAppDTO request, Integer operatorId);

  Result<ShowcaseApp> updateStatus(Integer appId, String status, Integer operatorId);

  Result<String> delete(Integer appId);
}
