package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestSaveOfficeDocumentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseOfficeDocumentUserOptionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.OfficeDocument;
import java.util.List;

public interface OfficeDocumentService {
  ApiResponse<List<OfficeDocument>> list(String docType, String keyword);

  ApiResponse<List<ResponseOfficeDocumentUserOptionDTO>> listUserOptions(String keyword);

  ApiResponse<Integer> create(RequestSaveOfficeDocumentDTO request);

  ApiResponse<String> update(Integer documentId, RequestSaveOfficeDocumentDTO request);

  byte[] exportDocx(Integer documentId);
}
