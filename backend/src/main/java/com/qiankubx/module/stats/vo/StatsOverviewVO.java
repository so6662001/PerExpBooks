package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatsOverviewVO {

    private BigDecimal totalExpense;

    private BigDecimal totalReimbursed;

    private BigDecimal totalPending;

    private Integer totalTripDays;

    private Integer tripCount;

    private Integer invoiceCount;
}
