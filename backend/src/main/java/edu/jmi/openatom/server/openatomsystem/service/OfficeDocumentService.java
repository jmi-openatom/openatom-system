package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveOfficeDocumentDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseOfficeDocumentUserOptionVO;
import edu.jmi.openatom.server.openatomsystem.entity.OfficeDocument;
import java.util.List;

/**
 * 公文管理服务接口
 *
 * <p>定义公文的列表查询, 用户选项查询, 创建, 更新和导出 docx 等业务操作
 */
public interface OfficeDocumentService {
  Result<List<OfficeDocument>> list(String docType, String keyword);

  Result<List<ResponseOfficeDocumentUserOptionVO>> listUserOptions(String keyword);

  Result<Integer> create(RequestSaveOfficeDocumentDTO request);

  Result<String> update(Integer documentId, RequestSaveOfficeDocumentDTO request);

  byte[] exportDocx(Integer documentId);
}
