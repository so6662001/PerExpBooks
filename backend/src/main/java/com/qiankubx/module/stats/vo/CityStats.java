package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CityStats {

    private String city;

    private Integer count;

    private BigDecimal amount;
}
