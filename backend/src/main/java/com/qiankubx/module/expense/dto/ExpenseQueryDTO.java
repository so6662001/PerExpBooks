package com.qiankubx.module.expense.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExpenseQueryDTO {

    private Integer status;

    private Long categoryId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer type;

    private Integer reimburseStatus;

    private Integer page = 1;

    private Integer pageSize = 20;
}
