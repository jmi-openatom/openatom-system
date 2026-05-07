package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestSaveOfficeDocumentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseOfficeDocumentUserOptionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.OfficeDocument;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.OfficeDocumentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.OfficeDocumentService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfficeDocumentServiceImpl implements OfficeDocumentService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final String DOC_TYPE_LEAVE = "leave_note";
  private static final String DOC_TYPE_VENUE = "venue_application";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日");

  private final OfficeDocumentMapper officeDocumentMapper;
  private final ClubMapper clubMapper;
  private final UserMapper userMapper;

  @Override
  public ApiResponse<List<OfficeDocument>> list(String docType, String keyword) {
    Club club = defaultClub();
    if (club == null) {
      return ApiResponse.error(404, "默认社团不存在");
    }
    LambdaQueryWrapper<OfficeDocument> wrapper =
        new LambdaQueryWrapper<OfficeDocument>()
            .eq(OfficeDocument::getClubId, club.getId())
            .orderByDesc(OfficeDocument::getId);
    if (!isBlank(docType)) {
      wrapper.eq(OfficeDocument::getDocType, normalizeDocType(docType));
    }
    if (!isBlank(keyword)) {
      wrapper.like(OfficeDocument::getTitle, keyword.trim());
    }
    return ApiResponse.success(officeDocumentMapper.selectList(wrapper));
  }

  @Override
  public ApiResponse<List<ResponseOfficeDocumentUserOptionDTO>> listUserOptions(String keyword) {
    LambdaQueryWrapper<User> wrapper =
        new LambdaQueryWrapper<User>()
            .select(
                User::getId,
                User::getRealName,
                User::getStudentId,
                User::getCollege,
                User::getMajor,
                User::getGrade,
                User::getClassName,
                User::getPhone)
            .orderByAsc(User::getStudentId)
            .orderByAsc(User::getId);
    if (!isBlank(keyword)) {
      wrapper.and(
          query ->
              query
                  .like(User::getRealName, keyword.trim())
                  .or()
                  .like(User::getStudentId, keyword.trim())
                  .or()
                  .like(User::getMajor, keyword.trim())
                  .or()
                  .like(User::getClassName, keyword.trim()));
    }
    return ApiResponse.success(
        userMapper.selectList(wrapper).stream()
            .map(
                item ->
                    ResponseOfficeDocumentUserOptionDTO.builder()
                        .id(item.getId())
                        .realName(item.getRealName())
                        .studentId(item.getStudentId())
                        .college(item.getCollege())
                        .major(item.getMajor())
                        .grade(item.getGrade())
                        .className(item.getClassName())
                        .phone(item.getPhone())
                        .build())
            .toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<Integer> create(RequestSaveOfficeDocumentDTO request) {
    Club club = defaultClub();
    if (club == null) {
      return ApiResponse.error(404, "默认社团不存在");
    }
    String docType = normalizeDocType(request == null ? null : request.getDocType());
    if (docType == null) {
      return ApiResponse.error(400, "不支持的单据类型");
    }
    ApiResponse<String> validation = validatePayload(docType, request.getPayload());
    if (validation != null) {
      return ApiResponse.error(validation.getCode(), validation.getMessage());
    }
    Integer operatorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    OfficeDocument document =
        OfficeDocument.builder()
            .clubId(club.getId())
            .docType(docType)
            .title(request.getTitle().trim())
            .status("draft")
            .payload(Jsons.stringify(request.getPayload()))
            .createdBy(operatorId)
            .updatedBy(operatorId)
            .build();
    int rows = officeDocumentMapper.insert(document);
    return rows > 0 ? ApiResponse.success(document.getId(), "单据创建成功") : ApiResponse.error("单据创建失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> update(Integer documentId, RequestSaveOfficeDocumentDTO request) {
    OfficeDocument document = officeDocumentMapper.selectById(documentId);
    if (document == null) {
      return ApiResponse.error(404, "单据不存在");
    }
    String docType = normalizeDocType(request == null ? null : request.getDocType());
    if (docType == null) {
      return ApiResponse.error(400, "不支持的单据类型");
    }
    ApiResponse<String> validation = validatePayload(docType, request.getPayload());
    if (validation != null) {
      return ApiResponse.error(validation.getCode(), validation.getMessage());
    }
    document.setDocType(docType);
    document.setTitle(request.getTitle().trim());
    document.setPayload(Jsons.stringify(request.getPayload()));
    document.setUpdatedBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null);
    int rows = officeDocumentMapper.updateById(document);
    return rows > 0 ? ApiResponse.success("单据更新成功") : ApiResponse.error("单据更新失败");
  }

  @Override
  public byte[] exportDocx(Integer documentId) {
    OfficeDocument document = officeDocumentMapper.selectById(documentId);
    if (document == null) {
      throw new IllegalArgumentException("单据不存在");
    }
    Map<String, Object> payload = Jsons.parseObject(document.getPayload());
    Map<Integer, User> selectedUsers = loadUsers(payload.get("memberIds"));
    try (XWPFDocument word = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (DOC_TYPE_LEAVE.equals(document.getDocType())) {
        buildLeaveNote(word, document, payload, selectedUsers);
      } else if (DOC_TYPE_VENUE.equals(document.getDocType())) {
        buildVenueApplication(word, document, payload, selectedUsers);
      } else {
        throw new IllegalArgumentException("不支持的单据类型");
      }
      word.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Word 导出失败");
    }
  }

  private void buildLeaveNote(
      XWPFDocument word,
      OfficeDocument document,
      Map<String, Object> payload,
      Map<Integer, User> selectedUsers) {
    addTitle(word, "假条");
    addParagraph(word, value(payload, "salutation", "尊敬的辅导员及任课老师："), 14, false);

    String dateRange =
        formatDate(value(payload, "leaveStartDate", null))
            + " 至 "
            + defaultText(value(payload, "leaveEndDate", null), "本学期结束");
    String venue = value(payload, "venueName", "至德楼 E206");
    String timeRange = value(payload, "timeRange", "18:30 - 20:00");
    String reason = value(payload, "reason", "开展社团项目培训与竞赛训练");
    addParagraph(
        word,
        "现需以下同学于 " + dateRange + "，每天 " + timeRange + " 到 " + venue + " 进行活动。事由如下：" + reason,
        12,
        false);

    addParagraph(word, "具体同学名单如下：", 12, true);
    for (String line : buildGroupLines(selectedUsers.values())) {
      addParagraph(word, line, 12, false);
    }

    addBlankParagraph(word);
    addParagraph(word, "指导老师签字：", 12, false);
    addParagraph(word, "导员签字：", 12, false);
    addBlankParagraph(word);
    addRightParagraph(word, value(payload, "departmentName", "信息工程学院"), 12);
    addRightParagraph(
        word,
        formatDate(value(payload, "documentDate", toDateString(document.getCreatedAt()))),
        12);
  }

  private void buildVenueApplication(
      XWPFDocument word,
      OfficeDocument document,
      Map<String, Object> payload,
      Map<Integer, User> selectedUsers) {
    addTitle(word, value(payload, "formTitle", "江苏海事职业技术学院实践场所预约申请表"));

    XWPFTable table = word.createTable(4, 2);
    table.setWidthType(TableWidthType.PCT);
    table.setWidth("100%");
    fillLabelCell(table.getRow(0).getCell(0), "申请信息");
    fillValueCell(table.getRow(0).getCell(1), document.getTitle());

    fillLabelCell(table.getRow(1).getCell(0), "实践场所");
    fillValueCell(
        table.getRow(1).getCell(1),
        "场地名称："
            + value(payload, "venueName", "-")
            + "\n"
            + "联系人："
            + value(payload, "contactName", "-")
            + "    联系电话："
            + value(payload, "contactPhone", "-")
            + "\n"
            + "申请开始日期："
            + formatDate(value(payload, "startDate", null))
            + "\n"
            + "申请结束日期："
            + formatDate(value(payload, "endDate", null))
            + "\n"
            + "总时长："
            + buildDurationText(payload));

    fillLabelCell(table.getRow(2).getCell(0), "实践项目");
    fillValueCell(
        table.getRow(2).getCell(1),
        "项目类型："
            + String.join("、", stringList(payload.get("projectTypes")))
            + "\n"
            + "项目简要说明："
            + value(payload, "projectDescription", "-"));

    fillLabelCell(table.getRow(3).getCell(0), "申请主体");
    fillValueCell(
        table.getRow(3).getCell(1),
        "申请方式："
            + ("team".equals(value(payload, "applyMode", "team")) ? "团队" : "个人")
            + "\n"
            + "指导老师："
            + value(payload, "teacherName", "-")
            + "    联系电话："
            + value(payload, "teacherPhone", "-")
            + "\n"
            + "人数："
            + selectedUsers.size()
            + " 人\n"
            + buildMemberDetail(selectedUsers.values()));

    addBlankParagraph(word);
    addParagraph(word, "我承诺：", 12, true);
    addParagraph(word, "1. 所填信息真实准确，无虚假隐瞒。", 12, false);
    addParagraph(word, "2. 活动结束后及时恢复场地原状，遵守学校相关管理要求。", 12, false);
    addParagraph(word, "3. 如需取消或调整预约，将提前联系相关负责人。", 12, false);
    addBlankParagraph(word);
    addParagraph(word, "申请人签字：__________________", 12, false);
    addParagraph(
        word,
        "日期：" + formatDate(value(payload, "documentDate", toDateString(document.getCreatedAt()))),
        12,
        false);
  }

  private ApiResponse<String> validatePayload(String docType, Map<String, Object> payload) {
    if (payload == null || payload.isEmpty()) {
      return ApiResponse.error(400, "单据内容不能为空");
    }
    if (stringList(payload.get("memberIds")).isEmpty()) {
      return ApiResponse.error(400, "请至少选择一名人员");
    }
    if (DOC_TYPE_LEAVE.equals(docType)) {
      if (isBlank(value(payload, "reason", null))) {
        return ApiResponse.error(400, "请填写请假事由");
      }
      return null;
    }
    if (DOC_TYPE_VENUE.equals(docType)) {
      if (isBlank(value(payload, "venueName", null))) {
        return ApiResponse.error(400, "请填写场地名称");
      }
      if (isBlank(value(payload, "contactName", null))) {
        return ApiResponse.error(400, "请填写联系人");
      }
      return null;
    }
    return ApiResponse.error(400, "不支持的单据类型");
  }

  private Club defaultClub() {
    return clubMapper.selectOne(
        new LambdaQueryWrapper<Club>().eq(Club::getCode, DEFAULT_CLUB_CODE));
  }

  private String normalizeDocType(String docType) {
    if (DOC_TYPE_LEAVE.equals(docType) || DOC_TYPE_VENUE.equals(docType)) {
      return docType;
    }
    return null;
  }

  private Map<Integer, User> loadUsers(Object rawMemberIds) {
    List<Integer> memberIds =
        stringList(rawMemberIds).stream()
            .map(this::parseInteger)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (memberIds.isEmpty()) {
      return Map.of();
    }
    return userMapper.selectBatchIds(memberIds).stream()
        .collect(
            Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
  }

  private List<String> buildGroupLines(Collection<User> users) {
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    for (User user : users) {
      String key = defaultText(user.getClassName(), "未分班");
      grouped
          .computeIfAbsent(key.trim(), item -> new ArrayList<>())
          .add(defaultText(user.getRealName(), "未命名"));
    }
    List<String> lines = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
      lines.add(entry.getKey() + "    " + String.join("、", entry.getValue()));
    }
    return lines;
  }

  private String buildDurationText(Map<String, Object> payload) {
    String totalHours = value(payload, "totalHours", "");
    String dailySchedule = value(payload, "dailySchedule", "");
    if (isBlank(totalHours) && isBlank(dailySchedule)) {
      return "-";
    }
    if (isBlank(dailySchedule)) {
      return totalHours + " 小时";
    }
    if (isBlank(totalHours)) {
      return dailySchedule;
    }
    return totalHours + " 小时（" + dailySchedule + "）";
  }

  private String buildMemberDetail(Collection<User> users) {
    if (users.isEmpty()) {
      return "名单：-";
    }
    StringBuilder builder = new StringBuilder("名单（姓名/学号/学院）：");
    for (User user : users) {
      builder
          .append("\n")
          .append(defaultText(user.getRealName(), "-"))
          .append("    ")
          .append(defaultText(user.getStudentId(), "-"))
          .append("    ")
          .append(defaultText(user.getCollege(), "-"));
    }
    return builder.toString();
  }

  private void addTitle(XWPFDocument word, String text) {
    XWPFParagraph paragraph = word.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = paragraph.createRun();
    run.setBold(true);
    run.setFontFamily("SimSun");
    run.setFontSize(18);
    run.setText(defaultText(text, "文书"));
  }

  private void addParagraph(XWPFDocument word, String text, int fontSize, boolean bold) {
    XWPFParagraph paragraph = word.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.LEFT);
    XWPFRun run = paragraph.createRun();
    run.setFontFamily("SimSun");
    run.setFontSize(fontSize);
    run.setBold(bold);
    run.setText(defaultText(text, ""));
  }

  private void addRightParagraph(XWPFDocument word, String text, int fontSize) {
    XWPFParagraph paragraph = word.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.RIGHT);
    XWPFRun run = paragraph.createRun();
    run.setFontFamily("SimSun");
    run.setFontSize(fontSize);
    run.setText(defaultText(text, ""));
  }

  private void addBlankParagraph(XWPFDocument word) {
    word.createParagraph();
  }

  private void fillLabelCell(XWPFTableCell cell, String text) {
    cell.removeParagraph(0);
    XWPFParagraph paragraph = cell.addParagraph();
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = paragraph.createRun();
    run.setFontFamily("SimSun");
    run.setBold(true);
    run.setText(text);
  }

  private void fillValueCell(XWPFTableCell cell, String text) {
    cell.removeParagraph(0);
    XWPFParagraph paragraph = cell.addParagraph();
    paragraph.setAlignment(ParagraphAlignment.LEFT);
    XWPFRun run = paragraph.createRun();
    run.setFontFamily("SimSun");
    run.setText(defaultText(text, "-"));
  }

  private String value(Map<String, Object> payload, String key, String defaultValue) {
    Object value = payload == null ? null : payload.get(key);
    return value == null ? defaultValue : String.valueOf(value).trim();
  }

  private List<String> stringList(Object value) {
    if (value instanceof Collection<?> collection) {
      return collection.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }
    if (value == null) {
      return List.of();
    }
    return List.of(String.valueOf(value));
  }

  private Integer parseInteger(String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private String formatDate(String value) {
    if (isBlank(value)) {
      return "-";
    }
    try {
      return LocalDate.parse(value).format(DATE_FORMATTER);
    } catch (RuntimeException ignored) {
      return value;
    }
  }

  private String toDateString(Timestamp timestamp) {
    if (timestamp == null) {
      return LocalDate.now().toString();
    }
    return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())
        .toLocalDate()
        .toString();
  }

  private String defaultText(String value, String defaultValue) {
    return isBlank(value) ? defaultValue : value;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
