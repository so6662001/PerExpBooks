package com.qiankubx.module.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String phone;

    private String nickname;

    private String avatarUrl;

    private String company;

    private String department;

    private String inviteCode;

    private Integer memberType;

    private Integer memberStatus;

    private LocalDateTime memberExpireTime;

    private LocalDateTime trialEndTime;

    private Long teamId;

    private Integer monthlyInvoiceUsed;

    private Integer monthlyReimburseUsed;

    private BigDecimal defaultSubsidy;

    private LocalDateTime createdAt;
}
