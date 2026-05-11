package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestSaveOfficeDocumentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseOfficeDocumentUserOptionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.OfficeDocument;
import java.util.List;

/**
 * 公文管理服务接口
 *
 * <p>定义公文的列表查询, 用户选项查询, 创建, 更新和导出 docx 等业务操作
 */
public interface OfficeDocumentService {
  ApiResponse<List<OfficeDocument>> list(String docType, String keyword);

  ApiResponse<List<ResponseOfficeDocumentUserOptionDTO>> listUserOptions(String keyword);

  ApiResponse<Integer> create(RequestSaveOfficeDocumentDTO request);

  ApiResponse<String> update(Integer documentId, RequestSaveOfficeDocumentDTO request);

  byte[] exportDocx(Integer documentId);
}
