package com.qiankubx.module.promotion.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardVO {

    private LevelInfoVO levelInfo;

    private BigDecimal totalCommission;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private Integer points;

    private Integer totalInvite;

    private Integer paidInvite;
}
