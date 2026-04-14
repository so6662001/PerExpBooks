package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TripSummaryVO {

    private Integer tripCount;

    private Integer totalDays;

    private BigDecimal totalSubsidy;

    private List<CityStats> cityDistribution;
}
