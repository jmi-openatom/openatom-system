package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveDocumentTemplateVariablesDTO;
import edu.jmi.openatom.server.openatomsystem.entity.DocumentTemplate;
import edu.jmi.openatom.server.openatomsystem.entity.GeneratedDocument;
import edu.jmi.openatom.server.openatomsystem.mapper.DocumentTemplateMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.GeneratedDocumentMapper;
import edu.jmi.openatom.server.openatomsystem.service.DocumentTemplateService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentTemplateServiceImpl implements DocumentTemplateService {
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}");
  private static final Pattern SINGLE_VARIABLE_PATTERN = Pattern.compile("^\\s*\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}\\s*$");
  private static final String DOC_FONT = "宋体";

  private final DocumentTemplateMapper documentTemplateMapper;
  private final GeneratedDocumentMapper generatedDocumentMapper;

  @Value("${app.document-template.storage-dir:./uploads/document-templates}")
  private String templateStorageDir;

  @Value("${app.generated-document.storage-dir:./uploads/generated-documents}")
  private String generatedStorageDir;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<DocumentTemplate> upload(String templateType, String templateName, MultipartFile file) {
    if (templateType == null || templateType.isBlank()) return Result.error(400, "模板类型不能为空");
    if (templateName == null || templateName.isBlank()) return Result.error(400, "模板名称不能为空");
    if (file == null || file.isEmpty()) return Result.error(400, "模板文件不能为空");
    String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    if (!originalName.toLowerCase().endsWith(".docx")) return Result.error(400, "仅支持上传 .docx 模板");
    try {
      Path root = root(templateStorageDir);
      Files.createDirectories(root);
      String storedName = UUID.randomUUID() + ".docx";
      Path target = root.resolve(storedName).normalize();
      try (InputStream input = file.getInputStream()) {
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
      }
      List<Map<String, Object>> variables = scanVariables(target);
      DocumentTemplate template =
          DocumentTemplate.builder()
              .templateType(templateType.trim())
              .templateName(templateName.trim())
              .version(1)
              .filePath(target.toString())
              .originalFileName(originalName)
              .variables(Jsons.stringify(variables))
              .status("enabled")
              .createdBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null)
              .build();
      documentTemplateMapper.insert(template);
      return Result.success(template, "模板上传成功");
    } catch (IOException e) {
      return Result.error(500, "模板上传失败: " + e.getMessage());
    }
  }

  @Override
  public Result<List<DocumentTemplate>> list(String templateType, String status) {
    return Result.success(documentTemplateMapper.selectByConditions(templateType, status));
  }

  @Override
  public Result<List<Map<String, Object>>> variables(Long templateId) {
    DocumentTemplate template = documentTemplateMapper.selectById(templateId);
    if (template == null) return Result.error(404, "模板不存在");
    return Result.success(Jsons.parseListOfObjects(template.getVariables()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> saveVariables(Long templateId, RequestSaveDocumentTemplateVariablesDTO request) {
    DocumentTemplate template = documentTemplateMapper.selectById(templateId);
    if (template == null) return Result.error(404, "模板不存在");
    template.setVariables(Jsons.stringify(request.getVariables()));
    return documentTemplateMapper.updateById(template) > 0
        ? Result.success("变量配置已保存")
        : Result.error("变量配置保存失败");
  }

  @Override
  public byte[] downloadGenerated(Long documentId) {
    GeneratedDocument document = generatedDocumentMapper.selectById(documentId);
    if (document == null) throw new IllegalArgumentException("生成文档不存在");
    try {
      Path path = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
      Path root = root(generatedStorageDir);
      if (!path.startsWith(root) || !Files.exists(path)) throw new IllegalArgumentException("文件不存在");
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new IllegalStateException("读取文档失败");
    }
  }

  @Override
  public GeneratedDocument generatedDocument(Long documentId) {
    return generatedDocumentMapper.selectById(documentId);
  }

  public List<Map<String, Object>> scanVariables(Path file) throws IOException {
    Set<String> keys = new LinkedHashSet<>();
    try (InputStream input = Files.newInputStream(file); XWPFDocument document = new XWPFDocument(input)) {
      collectVariables(document, keys);
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (String key : keys) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("key", key);
      item.put("label", key);
      item.put("required", true);
      item.put("defaultValue", "");
      result.add(item);
    }
    return result;
  }

  public Path generateDocx(DocumentTemplate template, Map<String, Object> variables, String fileName)
      throws IOException {
    Path source = Paths.get(template.getFilePath()).toAbsolutePath().normalize();
    if (!Files.exists(source)) throw new IOException("模板文件不存在");
    Path root = root(generatedStorageDir);
    Files.createDirectories(root);
    Path target = root.resolve(safeFileName(fileName)).normalize();
    if (!target.getParent().equals(root)) throw new IOException("生成文件名不合法");
    try (InputStream input = Files.newInputStream(source);
        XWPFDocument document = new XWPFDocument(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      replaceVariables(document, variables);
      document.write(out);
      Files.write(target, out.toByteArray());
      return target;
    }
  }

  public List<String> missingRequiredVariables(DocumentTemplate template, Map<String, Object> values) {
    List<String> missing = new ArrayList<>();
    for (Map<String, Object> variable : Jsons.parseListOfObjects(template.getVariables())) {
      Object required = variable.get("required");
      String key = String.valueOf(variable.getOrDefault("key", ""));
      if (key.isBlank()) continue;
      boolean isRequired = required == null || Boolean.parseBoolean(String.valueOf(required));
      Object value = values.get(key);
      if (isRequired && (value == null || String.valueOf(value).isBlank())) {
        missing.add(key);
      }
    }
    return missing;
  }

  private void collectVariables(XWPFDocument document, Set<String> keys) {
    for (XWPFParagraph paragraph : document.getParagraphs()) collectVariables(paragraph.getText(), keys);
    for (XWPFTable table : document.getTables()) collectVariables(table, keys);
    for (XWPFHeader header : document.getHeaderList()) {
      for (XWPFParagraph paragraph : header.getParagraphs()) collectVariables(paragraph.getText(), keys);
      for (XWPFTable table : header.getTables()) collectVariables(table, keys);
    }
    for (XWPFFooter footer : document.getFooterList()) {
      for (XWPFParagraph paragraph : footer.getParagraphs()) collectVariables(paragraph.getText(), keys);
      for (XWPFTable table : footer.getTables()) collectVariables(table, keys);
    }
  }

  private void collectVariables(XWPFTable table, Set<String> keys) {
    for (XWPFTableRow row : table.getRows()) {
      for (XWPFTableCell cell : row.getTableCells()) {
        for (XWPFParagraph paragraph : cell.getParagraphs()) collectVariables(paragraph.getText(), keys);
        for (XWPFTable nested : cell.getTables()) collectVariables(nested, keys);
      }
    }
  }

  private void collectVariables(String text, Set<String> keys) {
    if (text == null) return;
    Matcher matcher = VARIABLE_PATTERN.matcher(text);
    while (matcher.find()) keys.add(matcher.group(1));
  }

  private void replaceVariables(XWPFDocument document, Map<String, Object> variables) {
    for (XWPFParagraph paragraph : document.getParagraphs()) replaceParagraph(paragraph, variables);
    for (XWPFTable table : document.getTables()) replaceTable(table, variables);
    for (XWPFHeader header : document.getHeaderList()) {
      for (XWPFParagraph paragraph : header.getParagraphs()) replaceParagraph(paragraph, variables);
      for (XWPFTable table : header.getTables()) replaceTable(table, variables);
    }
    for (XWPFFooter footer : document.getFooterList()) {
      for (XWPFParagraph paragraph : footer.getParagraphs()) replaceParagraph(paragraph, variables);
      for (XWPFTable table : footer.getTables()) replaceTable(table, variables);
    }
  }

  private void replaceTable(XWPFTable table, Map<String, Object> variables) {
    for (XWPFTableRow row : table.getRows()) {
      for (XWPFTableCell cell : row.getTableCells()) {
        for (XWPFParagraph paragraph : cell.getParagraphs()) replaceParagraph(paragraph, variables);
        for (XWPFTable nested : cell.getTables()) replaceTable(nested, variables);
      }
    }
  }

  private void replaceParagraph(XWPFParagraph paragraph, Map<String, Object> variables) {
    String text = paragraph.getText();
    if (text == null || !text.contains("{{")) return;
    Matcher singleVariable = SINGLE_VARIABLE_PATTERN.matcher(text);
    if (singleVariable.matches() && "activityContentFull".equals(singleVariable.group(1))) {
      replaceActivityApplicationContent(paragraph, String.valueOf(variables.getOrDefault("activityContentFull", "")));
      return;
    }
    String replaced = replaceText(text, variables);
    List<XWPFRun> runs = paragraph.getRuns();
    if (runs.isEmpty()) {
      paragraph.createRun().setText(replaced);
      return;
    }
    runs.getFirst().setText(replaced, 0);
    for (int i = 1; i < runs.size(); i++) {
      runs.get(i).setText("", 0);
    }
  }

  private void replaceActivityApplicationContent(XWPFParagraph paragraph, String value) {
    clearRuns(paragraph);
    paragraph.setSpacingBetween(1.25);
    paragraph.setSpacingBefore(0);
    paragraph.setSpacingAfter(120);
    String[] lines = value == null ? new String[0] : value.replace("\r\n", "\n").replace('\r', '\n').split("\\n");
    boolean firstWritten = false;
    for (String rawLine : lines) {
      String line = rawLine == null ? "" : rawLine.trim();
      if (line.isBlank()) {
        if (firstWritten) paragraph.createRun().addBreak();
        continue;
      }
      if (firstWritten) paragraph.createRun().addBreak();
      boolean heading = isApplicationSectionHeading(line);
      XWPFRun run = paragraph.createRun();
      run.setFontFamily(DOC_FONT);
      run.setFontSize(heading ? 12 : 11);
      run.setBold(heading);
      run.setText(line);
      firstWritten = true;
    }
  }

  private boolean isApplicationSectionHeading(String line) {
    return line != null && line.matches("^[一二三四五六七八九十]+、.{2,18}$");
  }

  private void clearRuns(XWPFParagraph paragraph) {
    for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
      paragraph.removeRun(i);
    }
  }

  private String replaceText(String text, Map<String, Object> variables) {
    Matcher matcher = VARIABLE_PATTERN.matcher(text);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String key = matcher.group(1);
      Object value = variables.get(key);
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private Path root(String dir) {
    return Paths.get(dir).toAbsolutePath().normalize();
  }

  private String safeFileName(String fileName) {
    String value = fileName == null || fileName.isBlank() ? UUID.randomUUID() + ".docx" : fileName;
    value = value.replaceAll("[\\\\/:*?\"<>|]+", "-");
    if (!value.toLowerCase().endsWith(".docx")) value += ".docx";
    return value;
  }
}
