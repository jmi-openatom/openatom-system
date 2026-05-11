package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateFormSubmissionDTO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseFormSubmissionVO;

/**
 * 表单提交服务接口
 *
 * <p>定义表单提交记录的创建, 分页列表查询以及导出 Excel 等业务操作
 */
public interface FormSubmissionService {
  Result<Integer> create(Integer formId, RequestCreateFormSubmissionDTO request);

  Result<PageDataVO<ResponseFormSubmissionVO>> list(
      Integer formId, String keyword, Long page, Long pageSize);

  byte[] exportExcel(Integer formId);
}
