package com.qiankubx.module.agreement.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgreementCheckVO {

    private Boolean needConsent;

    private Boolean block;

    private List<AgreementItem> agreements;

    @Data
    public static class AgreementItem {

        private String type;

        private Long versionId;

        private String versionCode;

        private String changeSummary;

        private String changeLevel;
    }
}
