package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalendarDayVO {

    private String date;

    private BigDecimal amount;

    private Integer count;
}
