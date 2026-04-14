package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategoryRatioVO {

    private Long categoryId;

    private String categoryName;

    private BigDecimal amount;

    private BigDecimal ratio;
}
