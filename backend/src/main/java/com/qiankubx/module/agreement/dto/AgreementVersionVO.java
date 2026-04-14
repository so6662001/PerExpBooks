package com.qiankubx.module.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgreementVersionVO {

    private Long id;

    private String type;

    private String versionCode;

    private String title;

    private String content;

    private String changeSummary;

    private String changeLevel;

    private LocalDateTime effectiveAt;

    private LocalDateTime createdAt;
}
