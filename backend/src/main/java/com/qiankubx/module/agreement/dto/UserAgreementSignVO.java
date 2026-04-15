package com.qiankubx.module.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAgreementSignVO {

    private Long id;

    private String agreementType;

    private String versionCode;

    private String ipAddress;

    private LocalDateTime signedAt;
}
