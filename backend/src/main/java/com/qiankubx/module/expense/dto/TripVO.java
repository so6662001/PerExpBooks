package com.qiankubx.module.expense.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TripVO {

    private Long id;

    private Long userId;

    private String title;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer days;

    private BigDecimal subsidyPerDay;

    private BigDecimal subsidyTotal;

    private String remark;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer expenseCount;
}
