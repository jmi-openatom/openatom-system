package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateFormSubmissionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseFormSubmissionDTO;

/**
 * 表单提交服务接口
 *
 * <p>定义表单提交记录的创建, 分页列表查询以及导出 Excel 等业务操作
 */
public interface FormSubmissionService {
  ApiResponse<Integer> create(Integer formId, RequestCreateFormSubmissionDTO request);

  ApiResponse<PageDataDTO<ResponseFormSubmissionDTO>> list(
      Integer formId, String keyword, Long page, Long pageSize);

  byte[] exportExcel(Integer formId);
}
