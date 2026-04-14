package com.qiankubx.module.member.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Integer planType;

    private String planName;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private Integer payType;

    private Integer payStatus;

    private LocalDateTime payTime;

    private String tradeNo;

    private LocalDateTime memberStart;

    private LocalDateTime memberEnd;

    private Integer isRenewal;

    private Integer refundStatus;

    private LocalDateTime createdAt;
}
