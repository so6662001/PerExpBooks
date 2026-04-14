package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class YearlyCompareVO {

    private String month;

    private BigDecimal currentYear;

    private BigDecimal lastYear;
}
