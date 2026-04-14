package com.qiankubx.module.promotion.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceVO {

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal totalCommission;

    private BigDecimal withdrawnAmount;
}
