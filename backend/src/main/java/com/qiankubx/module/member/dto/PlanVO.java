package com.qiankubx.module.member.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanVO {

    private Integer planType;

    private String name;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private List<String> features;

    private boolean recommended;
}
