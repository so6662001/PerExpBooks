package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReimburseProgressVO {

    private String month;

    private BigDecimal submitted;

    private BigDecimal received;

    private BigDecimal pending;
}
