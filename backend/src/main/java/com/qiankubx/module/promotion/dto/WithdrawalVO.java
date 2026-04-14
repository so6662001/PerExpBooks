package com.qiankubx.module.promotion.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawalVO {

    private Long id;

    private BigDecimal amount;

    private Integer withdrawType;

    private String withdrawTypeName;

    private Integer status;

    private String statusName;

    private String rejectReason;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
}
