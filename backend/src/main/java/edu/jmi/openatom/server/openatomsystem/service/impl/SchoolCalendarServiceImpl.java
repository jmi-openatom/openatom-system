package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveSchoolCalendarAdjustmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveSchoolCalendarDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SchoolCalendarAdjustment;
import edu.jmi.openatom.server.openatomsystem.entity.SchoolCalendarSetting;
import edu.jmi.openatom.server.openatomsystem.mapper.SchoolCalendarAdjustmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.SchoolCalendarSettingMapper;
import edu.jmi.openatom.server.openatomsystem.service.SchoolCalendarService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSchoolCalendarDayVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSchoolCalendarVO;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchoolCalendarServiceImpl implements SchoolCalendarService {
  private final SchoolCalendarSettingMapper settingMapper;
  private final SchoolCalendarAdjustmentMapper adjustmentMapper;

  @Override
  public Result<ResponseSchoolCalendarVO> detail() {
    return Result.success(buildCalendar());
  }

  @Override
  public Result<ResponseSchoolCalendarVO> save(RequestSaveSchoolCalendarDTO request) {
    LocalDate startDate = parseDate(request.getStartDate());
    if (startDate == null) return Result.error(400, "开学日期格式不正确");
    SchoolCalendarSetting setting = settingMapper.selectCurrent();
    if (setting == null) {
      setting = SchoolCalendarSetting.builder()
          .startDate(Date.valueOf(startDate))
          .weekCount(request.getWeekCount())
          .updatedBy(currentUserId())
          .build();
      settingMapper.insert(setting);
    } else {
      setting.setStartDate(Date.valueOf(startDate));
      setting.setWeekCount(request.getWeekCount());
      setting.setUpdatedBy(currentUserId());
      settingMapper.updateById(setting);
    }
    return Result.success(buildCalendar(), "校历已保存");
  }

  @Override
  public Result<ResponseSchoolCalendarVO> saveAdjustment(RequestSaveSchoolCalendarAdjustmentDTO request) {
    LocalDate date = parseDate(request.getDate());
    if (date == null) return Result.error(400, "调休日期格式不正确");
    String type = request.getType() == null ? "" : request.getType().trim();
    if (!"workday".equals(type) && !"restday".equals(type)) {
      return Result.error(400, "调休类型不合法");
    }
    Date sqlDate = Date.valueOf(date);
    SchoolCalendarAdjustment adjustment = adjustmentMapper.selectByDate(sqlDate);
    if (adjustment == null) {
      adjustment = SchoolCalendarAdjustment.builder()
          .calendarDate(sqlDate)
          .type(type)
          .reason(trimToNull(request.getReason()))
          .updatedBy(currentUserId())
          .build();
      adjustmentMapper.insert(adjustment);
    } else {
      adjustment.setType(type);
      adjustment.setReason(trimToNull(request.getReason()));
      adjustment.setUpdatedBy(currentUserId());
      adjustmentMapper.updateById(adjustment);
    }
    return Result.success(buildCalendar(), "调休已保存");
  }

  @Override
  public Result<String> deleteAdjustment(Integer adjustmentId) {
    if (adjustmentId == null) return Result.error(400, "调休记录不合法");
    int rows = adjustmentMapper.deleteById(adjustmentId);
    return rows > 0 ? Result.success("调休已删除") : Result.error(404, "调休记录不存在");
  }

  private ResponseSchoolCalendarVO buildCalendar() {
    SchoolCalendarSetting setting = settingMapper.selectCurrent();
    if (setting == null || setting.getStartDate() == null || setting.getWeekCount() == null) {
      return ResponseSchoolCalendarVO.builder().weekCount(0).days(List.of()).build();
    }
    LocalDate start = setting.getStartDate().toLocalDate();
    int weekCount = setting.getWeekCount();
    LocalDate end = start.plusDays((long) weekCount * 7 - 1);
    Map<LocalDate, SchoolCalendarAdjustment> adjustmentMap = adjustmentMapper.selectAllOrdered().stream()
        .collect(Collectors.toMap(item -> item.getCalendarDate().toLocalDate(), Function.identity(), (a, b) -> a));
    List<ResponseSchoolCalendarDayVO> days = start.datesUntil(end.plusDays(1))
        .map(date -> toDayVO(start, date, adjustmentMap.get(date)))
        .toList();
    return ResponseSchoolCalendarVO.builder()
        .id(setting.getId())
        .startDate(start.toString())
        .weekCount(weekCount)
        .endDate(end.toString())
        .days(days)
        .build();
  }

  private ResponseSchoolCalendarDayVO toDayVO(
      LocalDate start, LocalDate date, SchoolCalendarAdjustment adjustment) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    boolean restDay = weekend;
    String source = weekend ? "auto" : "normal";
    String adjustmentType = null;
    String reason = null;
    if (adjustment != null) {
      adjustmentType = adjustment.getType();
      reason = adjustment.getReason();
      restDay = "restday".equals(adjustment.getType());
      source = "adjustment";
    }
    int weekIndex = (int) ((date.toEpochDay() - start.toEpochDay()) / 7) + 1;
    return ResponseSchoolCalendarDayVO.builder()
        .date(date.toString())
        .weekIndex(weekIndex)
        .dayOfWeek(dayOfWeek.getValue())
        .dayName(dayName(dayOfWeek))
        .restDay(restDay)
        .source(source)
        .adjustmentType(adjustmentType)
        .reason(reason)
        .build();
  }

  private String dayName(DayOfWeek dayOfWeek) {
    return switch (dayOfWeek) {
      case MONDAY -> "周一";
      case TUESDAY -> "周二";
      case WEDNESDAY -> "周三";
      case THURSDAY -> "周四";
      case FRIDAY -> "周五";
      case SATURDAY -> "周六";
      case SUNDAY -> "周日";
    };
  }

  private LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return LocalDate.parse(value.trim());
    } catch (RuntimeException e) {
      return null;
    }
  }

  private Integer currentUserId() {
    return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
