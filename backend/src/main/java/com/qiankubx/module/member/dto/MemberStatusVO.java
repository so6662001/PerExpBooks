package com.qiankubx.module.member.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberStatusVO {

    private Integer memberType;

    private Integer memberStatus;

    private LocalDateTime expireTime;

    private Integer daysLeft;

    private QuotaInfo quotaInfo;

    @Data
    public static class QuotaInfo {
        private Integer monthlyInvoiceUsed;
        private Integer monthlyInvoiceLimit;
        private Integer monthlyReimburseUsed;
        private Integer monthlyReimburseLimit;
    }
}
