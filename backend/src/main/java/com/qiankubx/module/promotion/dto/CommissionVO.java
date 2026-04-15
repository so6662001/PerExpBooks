package com.qiankubx.module.promotion.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CommissionVO {

    private Long id;

    private Long inviteeId;

    private String inviteeNickname;

    private Integer level;

    private Integer commissionType;

    private BigDecimal orderAmount;

    private BigDecimal commissionRate;

    private BigDecimal commissionAmount;

    private Integer status;

    private String statusName;

    private LocalDateTime settleTime;

    private LocalDateTime createdAt;
}
