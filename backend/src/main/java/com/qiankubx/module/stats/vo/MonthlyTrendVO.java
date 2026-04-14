package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyTrendVO {

    private String month;

    private BigDecimal amount;
}
