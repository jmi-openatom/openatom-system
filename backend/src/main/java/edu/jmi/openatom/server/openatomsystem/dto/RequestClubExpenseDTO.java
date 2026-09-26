package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 已付款支出登记与修改请求。修改时 version 用于避免覆盖其他管理员的修改。 */
public record RequestClubExpenseDTO(
    @NotNull(message = "请选择所属社团") Integer clubId,
    @NotNull(message = "请选择付款日期") LocalDate paidOn,
    @NotBlank(message = "请填写支出事项") @Size(max = 160) String title,
    @NotBlank(message = "请选择支出分类") String category,
    @NotNull(message = "请填写支出金额") @DecimalMin(value = "0.01", message = "金额必须大于零")
        @Digits(integer = 10, fraction = 2, message = "金额最多10位整数和2位小数") BigDecimal amount,
    @NotBlank(message = "请填写经手人") @Size(max = 80) String handledBy,
    String paymentMethod,
    Integer activityId,
    @Size(max = 1000) String note,
    Integer version) {}
