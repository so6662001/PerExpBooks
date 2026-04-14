package com.qiankubx.module.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TripCreateDTO {

    @NotBlank(message = "出差标题不能为空")
    private String title;

    @NotBlank(message = "目的地不能为空")
    private String destination;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "每日补贴不能为空")
    private BigDecimal subsidyPerDay;

    private String remark;
}
