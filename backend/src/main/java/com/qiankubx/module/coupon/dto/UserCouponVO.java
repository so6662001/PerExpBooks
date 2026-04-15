package com.qiankubx.module.coupon.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;

    private String name;

    private Integer type;

    private String typeName;

    private BigDecimal discountValue;

    private BigDecimal minAmount;

    private Integer applicablePlanType;

    private Integer useStatus;

    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
