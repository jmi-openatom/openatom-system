package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveDocumentTemplateVariablesDTO;
import edu.jmi.openatom.server.openatomsystem.entity.DocumentTemplate;
import edu.jmi.openatom.server.openatomsystem.entity.GeneratedDocument;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentTemplateService {
  Result<DocumentTemplate> upload(String templateType, String templateName, MultipartFile file);

  Result<List<DocumentTemplate>> list(String templateType, String status);

  Result<List<Map<String, Object>>> variables(Long templateId);

  Result<String> saveVariables(Long templateId, RequestSaveDocumentTemplateVariablesDTO request);

  byte[] downloadGenerated(Long documentId);

  GeneratedDocument generatedDocument(Long documentId);
}
