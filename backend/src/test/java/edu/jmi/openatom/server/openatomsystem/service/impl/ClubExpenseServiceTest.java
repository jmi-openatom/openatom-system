package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.dto.RequestClubExpenseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ClubExpenseServiceTest {
  private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
  private final ClubExpenseAccess access = mock(ClubExpenseAccess.class);
  private final ClubExpenseService service = new ClubExpenseService(jdbc, null, new ObjectMapper(), access);

  @Test
  void rejectsAmountsThatWouldLoseCentsBeforeWriting() {
    for (String value : List.of("0", "-1", "0.001", "10000000000.00")) {
      ClubExpenseException error = assertThrows(ClubExpenseException.class,
          () -> service.create(request(new BigDecimal(value))));
      assertEquals(400, error.getCode(), value);
    }
    verifyNoInteractions(jdbc, access);
  }

  @Test
  void listWithoutClubIdOnlyQueriesCurrentMembershipClubs() {
    when(access.allowedClubIds()).thenReturn(List.of(2, 5));
    when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(0L);
    when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

    var result = service.list(new ClubExpenseService.Filter(null, null, null, null, null, "active", null), 1L, 20L);

    assertEquals(0L, result.getTotal());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
    verify(jdbc).queryForObject(sql.capture(), params.capture(), eq(Long.class));
    assertTrue(sql.getValue().contains("e.club_id IN (:allowedClubIds)"));
    assertEquals(List.of(2, 5), params.getValue().getValue("allowedClubIds"));
    assertEquals("active", params.getValue().getValue("status"));
  }

  @Test
  void explicitForeignClubIsRejectedBeforeAnyExpenseQuery() {
    doThrow(new ClubExpenseException(403, "无权访问该社团的支出")).when(access).checkClub(9);

    ClubExpenseException error = assertThrows(ClubExpenseException.class,
        () -> service.list(new ClubExpenseService.Filter(9, null, null, null, null, "active", null), 1L, 10L));

    assertEquals(403, error.getCode());
    verifyNoInteractions(jdbc);
  }

  @Test
  void summaryAlwaysExcludesVoidedRowsEvenWhenListShowsAllStatuses() {
    when(jdbc.queryForMap(anyString(), any(MapSqlParameterSource.class)))
        .thenReturn(Map.of("total_count", 0L, "total_amount", BigDecimal.ZERO));
    when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    var totals = service.summary(new ClubExpenseService.Filter(null, null, null, null, null, "all", null));

    assertEquals(BigDecimal.ZERO, totals.totalAmount());
    ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
    verify(jdbc).queryForMap(anyString(), params.capture());
    assertEquals("active", params.getValue().getValue("status"));
  }

  @Test
  void rejectsActivityFromAnotherClubBeforeCreatingExpense() {
    when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
        .thenReturn(0);
    RequestClubExpenseDTO request = new RequestClubExpenseDTO(1, LocalDate.of(2026, 9, 26),
        "活动物资", "materials", new BigDecimal("100.00"), "张同学", "wechat", 99, null, null);

    ClubExpenseException error = assertThrows(ClubExpenseException.class, () -> service.create(request));

    assertEquals(400, error.getCode());
    assertEquals("关联活动不属于该社团", error.getMessage());
  }

  @Test
  void csvCellsCannotBecomeSpreadsheetFormulas() {
    assertEquals("\"'=SUM(1,1)\"", ClubExpenseService.csvCell("=SUM(1,1)"));
    assertEquals("\"'  =SUM(1,1)\"", ClubExpenseService.csvCell("  =SUM(1,1)"));
    assertEquals("\"普通事项\"", ClubExpenseService.csvCell("普通事项"));
    assertEquals("\"甲\"\"乙\"", ClubExpenseService.csvCell("甲\"乙"));
  }

  private RequestClubExpenseDTO request(BigDecimal amount) {
    return new RequestClubExpenseDTO(1, LocalDate.of(2026, 9, 26), "物资采购",
        "materials", amount, "张同学", "wechat", null, null, null);
  }
}
