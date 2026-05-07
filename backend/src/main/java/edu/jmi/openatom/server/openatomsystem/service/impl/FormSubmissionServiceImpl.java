package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateFormSubmissionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseFormSubmissionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.FormSubmission;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.FormSubmissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.SiteFormMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.FormSubmissionService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FormSubmissionServiceImpl implements FormSubmissionService {
  private final FormSubmissionMapper formSubmissionMapper;
  private final SiteFormMapper siteFormMapper;
  private final UserMapper userMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<Integer> create(Integer formId, RequestCreateFormSubmissionDTO request) {
    SiteForm form = siteFormMapper.selectById(formId);
    if (form == null) return ApiResponse.error(404, "表单不存在");
    if (!List.of("open", "published").contains(form.getStatus())) return ApiResponse.error(400, "当前表单未开放提交");
    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (form.getStartAt() != null && now.before(form.getStartAt())) return ApiResponse.error(400, "表单收集尚未开始");
    if (form.getEndAt() != null && now.after(form.getEndAt())) return ApiResponse.error(400, "表单收集已结束");
    Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    if (Boolean.TRUE.equals(form.getLoginRequired()) && userId == null) return ApiResponse.error(401, "该表单需要登录后提交");
    if (userId == null) {
      if (isBlank(request.getAnonymousName())) return ApiResponse.error(400, "请填写联系人姓名");
      if (isBlank(request.getAnonymousContact())) return ApiResponse.error(400, "请填写联系方式");
    }
    ApiResponse<String> validation = validateFormData(form.getFormSchema(), request.getFormData());
    if (validation != null) return ApiResponse.error(validation.getCode(), validation.getMessage());
    FormSubmission submission = FormSubmission.builder().formId(formId).clubId(form.getClubId()).userId(userId)
        .anonymousName(trimToNull(request.getAnonymousName())).anonymousContact(trimToNull(request.getAnonymousContact()))
        .formData(Jsons.stringify(request.getFormData())).status("submitted").build();
    int row = formSubmissionMapper.insert(submission);
    return row > 0 ? ApiResponse.success(submission.getId(), "表单提交成功") : ApiResponse.error("表单提交失败");
  }

  @Override
  public ApiResponse<PageDataDTO<ResponseFormSubmissionDTO>> list(Integer formId, String keyword, Long page, Long pageSize) {
    if (formId == null) return ApiResponse.error(400, "formId不能为空");
    SiteForm form = siteFormMapper.selectById(formId);
    if (form == null) return ApiResponse.error(404, "表单不存在");
    long current = PageRequests.page(page);
    long size = PageRequests.pageSize(pageSize);
    LambdaQueryWrapper<FormSubmission> wrapper = new LambdaQueryWrapper<FormSubmission>()
        .eq(FormSubmission::getFormId, formId).orderByDesc(FormSubmission::getId);
    if (!isBlank(keyword)) {
      String trimmed = keyword.trim();
      List<Integer> userIds = userMapper.searchByKeyword(trimmed).stream().map(User::getId).toList();
      if (userIds.isEmpty()) {
        wrapper.and(q -> q.like(FormSubmission::getAnonymousName, trimmed).or().like(FormSubmission::getAnonymousContact, trimmed));
      } else {
        wrapper.and(q -> q.in(FormSubmission::getUserId, userIds).or().like(FormSubmission::getAnonymousName, trimmed).or().like(FormSubmission::getAnonymousContact, trimmed));
      }
    }
    Page<FormSubmission> submissionPage = formSubmissionMapper.selectPage(new Page<>(current, size), wrapper);
    return ApiResponse.success(PageDataDTO.<ResponseFormSubmissionDTO>builder().list(toResponseList(submissionPage.getRecords()))
        .page(submissionPage.getCurrent()).pageSize(submissionPage.getSize()).total(submissionPage.getTotal()).build());
  }

  @Override
  public byte[] exportExcel(Integer formId) {
    SiteForm form = siteFormMapper.selectById(formId);
    if (form == null) throw new IllegalArgumentException("表单不存在");
    List<FormSubmission> submissions = formSubmissionMapper.selectByFormIdOrdered(formId);
    List<Map<String, Object>> schema = Jsons.parseListOfObjects(form.getFormSchema());
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(safeSheetName(form.getName()));
      writeHeader(sheet, schema);
      List<ResponseFormSubmissionDTO> rows = toResponseList(submissions);
      for (int i = 0; i < rows.size(); i++) writeRow(sheet.createRow(i + 1), rows.get(i), schema);
      for (int column = 0; column < 4 + schema.size(); column++) sheet.autoSizeColumn(column);
      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) { throw new IllegalArgumentException("Excel 导出失败"); }
  }

  private void writeHeader(Sheet sheet, List<Map<String, Object>> schema) {
    Row header = sheet.createRow(0);
    header.createCell(0).setCellValue("提交时间"); header.createCell(1).setCellValue("提交人");
    header.createCell(2).setCellValue("账号"); header.createCell(3).setCellValue("联系方式");
    for (int i = 0; i < schema.size(); i++) header.createCell(i + 4).setCellValue(asString(schema.get(i).get("label"), "字段" + (i + 1)));
  }

  private void writeRow(Row row, ResponseFormSubmissionDTO submission, List<Map<String, Object>> schema) {
    row.createCell(0).setCellValue(submission.getCreatedAt() == null ? "" : submission.getCreatedAt().toString());
    row.createCell(1).setCellValue(defaultString(submission.getSubmitterName()));
    row.createCell(2).setCellValue(defaultString(submission.getSubmitterAccount()));
    row.createCell(3).setCellValue(defaultString(submission.getContact()));
    Map<String, Object> formData = submission.getFormData() == null ? Map.of() : submission.getFormData();
    for (int i = 0; i < schema.size(); i++) { String key = asString(schema.get(i).get("key"), ""); row.createCell(i + 4).setCellValue(formatValue(formData.get(key))); }
  }

  private ApiResponse<String> validateFormData(String formSchema, Map<String, Object> formData) {
    Map<String, Object> values = formData == null ? Map.of() : formData;
    for (Map<String, Object> field : Jsons.parseListOfObjects(formSchema)) {
      if (Boolean.TRUE.equals(field.get("required"))) {
        String key = asString(field.get("key"), "");
        if (key.isBlank()) continue;
        Object value = values.get(key);
        if (value == null || formatValue(value).isBlank()) return ApiResponse.error(400, "请填写" + asString(field.get("label"), key));
      }
    }
    return null;
  }

  private List<ResponseFormSubmissionDTO> toResponseList(List<FormSubmission> submissions) {
    if (submissions == null || submissions.isEmpty()) return List.of();
    Set<Integer> userIds = submissions.stream().map(FormSubmission::getUserId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Integer, User> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    return submissions.stream().map(s -> toResponse(s, users.get(s.getUserId()))).toList();
  }

  private ResponseFormSubmissionDTO toResponse(FormSubmission submission, User user) {
    Map<String, Object> formData = new LinkedHashMap<>(Jsons.parseObject(submission.getFormData()));
    String submitterName = user == null ? submission.getAnonymousName() : (isBlank(user.getRealName()) ? user.getUserName() : user.getRealName());
    String contact = user == null ? submission.getAnonymousContact() : firstNonBlank(user.getPhone(), user.getEmail(), submission.getAnonymousContact());
    return ResponseFormSubmissionDTO.builder().id(submission.getId()).formId(submission.getFormId()).clubId(submission.getClubId())
        .userId(submission.getUserId()).submitterName(submitterName).submitterAccount(user == null ? null : user.getUserName())
        .contact(contact).formData(formData).createdAt(submission.getCreatedAt()).build();
  }

  private String safeSheetName(String value) { String name = defaultString(value); if (name.isBlank()) return "表单记录"; return name.replaceAll("[\\\\/?*\\[\\]:]]", "_"); }

  private String formatValue(Object value) {
    if (value == null) return "";
    if (value instanceof List<?> list) return list.stream().map(this::formatValue).filter(item -> !item.isBlank()).collect(Collectors.joining(" / "));
    if (value instanceof Map<?, ?> map) return Jsons.stringify(map);
    return String.valueOf(value);
  }

  private String asString(Object value, String fallback) { String s = value == null ? "" : String.valueOf(value); return s.isBlank() ? fallback : s; }
  private String trimToNull(String value) { return isBlank(value) ? null : value.trim(); }
  private String firstNonBlank(String... values) { for (String v : values) if (!isBlank(v)) return v; return null; }
  private String defaultString(String value) { return value == null ? "" : value; }
  private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
