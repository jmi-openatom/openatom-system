package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPosition;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipExportTest {
  @Mock private ClubMembershipMapper membershipMapper;
  @Mock private UserMapper userMapper;
  @Mock private ClubMapper clubMapper;
  @Mock private ClubDepartmentMapper departmentMapper;
  @Mock private ClubPositionMapper positionMapper;
  @InjectMocks private MembershipServiceImpl service;

  @Test
  void exportsAllMatchingMembersWithProfileFieldsAndReadableFormatting() throws Exception {
    var members = IntStream.rangeClosed(1, 25).mapToObj(id -> ClubMembership.builder()
        .id(id).userId(id).clubId(2).departmentId(3).positionId(4).status("probation")
        .joinedAt(Times.parseTimestamp("2026-10-16T08:00:00+08:00")).build()).toList();
    var users = IntStream.rangeClosed(1, 25).mapToObj(id -> User.builder()
        .id(id).realName(id == 1 ? "=示例姓名" : "成员" + id).userName("user" + id)
        .studentId("001234567890123456789").phone("013800000000")
        .college("信息学院").major("软件技术").grade("2026").className("软件一班")
        .email("member@example.com").build()).toList();
    when(userMapper.searchByNameKeyword("成员")).thenReturn(users);
    var userIds = users.stream().map(User::getId).toList();
    when(membershipMapper.selectByConditions(2, 3, 4, "probation", userIds)).thenReturn(members);
    when(userMapper.selectBatchIds(anyCollection())).thenReturn(users);
    when(clubMapper.selectBatchIds(anyCollection())).thenReturn(
        List.of(Club.builder().id(2).name("开放原子开源社团").build()));
    when(departmentMapper.selectBatchIds(anyCollection())).thenReturn(
        List.of(ClubDepartment.builder().id(3).name("社区部").build()));
    when(positionMapper.selectBatchIds(anyCollection())).thenReturn(
        List.of(ClubPosition.builder().id(4).name("成员").build()));

    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(
        service.exportExcel(2, 3, 4, "probation", "成员")))) {
      var sheet = workbook.getSheet("社团人员名单");
      assertEquals(25, sheet.getLastRowNum());
      assertEquals(16, sheet.getRow(0).getLastCellNum());
      assertEquals("姓名", sheet.getRow(0).getCell(1).getStringCellValue());
      var row = sheet.getRow(1);
      assertEquals("=示例姓名", row.getCell(1).getStringCellValue());
      assertEquals(CellType.STRING, row.getCell(1).getCellType());
      assertEquals("001234567890123456789", row.getCell(3).getStringCellValue());
      assertEquals(CellType.STRING, row.getCell(3).getCellType());
      assertEquals("信息学院", row.getCell(4).getStringCellValue());
      assertEquals("软件一班", row.getCell(7).getStringCellValue());
      assertEquals("013800000000", row.getCell(8).getStringCellValue());
      assertEquals("member@example.com", row.getCell(9).getStringCellValue());
      assertEquals("开放原子开源社团", row.getCell(10).getStringCellValue());
      assertEquals("社区部", row.getCell(11).getStringCellValue());
      assertEquals("成员", row.getCell(12).getStringCellValue());
      assertEquals("试用", row.getCell(13).getStringCellValue());
      assertEquals("2026-10-16 08:00:00", row.getCell(14).getStringCellValue());
      assertEquals("", row.getCell(15).getStringCellValue());
      assertTrue(sheet.getPaneInformation().isFreezePane());
      assertEquals("A1:P26", sheet.getCTWorksheet().getAutoFilter().getRef());
    }
    verify(membershipMapper).selectByConditions(2, 3, 4, "probation", userIds);
  }

  @Test
  void unmatchedKeywordExportsHeadersWithoutIncludingOtherMembers() throws Exception {
    when(userMapper.searchByNameKeyword("不存在")).thenReturn(List.of());
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(
        service.exportExcel(2, null, null, null, "不存在")))) {
      assertEquals(0, workbook.getSheetAt(0).getLastRowNum());
      assertEquals("序号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
    }
    org.mockito.Mockito.verifyNoInteractions(membershipMapper);
  }

  @Test
  void missingProfileAndDepartmentStillProduceAValidRoster() throws Exception {
    when(membershipMapper.selectByConditions(2, null, null, null, null)).thenReturn(List.of(
        ClubMembership.builder().id(1).userId(99).clubId(2).status("active").build()));
    when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of());
    when(clubMapper.selectBatchIds(anyCollection())).thenReturn(List.of());
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(
        service.exportExcel(2, null, null, null, null)))) {
      var row = workbook.getSheetAt(0).getRow(1);
      assertEquals("用户 99", row.getCell(1).getStringCellValue());
      assertEquals("", row.getCell(3).getStringCellValue());
      assertEquals("", row.getCell(11).getStringCellValue());
      assertEquals("在会", row.getCell(13).getStringCellValue());
    }
  }
}
