package com.qiankubx.module.analytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ErrorReportDTO {

    @NotEmpty(message = "异常数据列表不能为空")
    @Valid
    private List<ErrorItem> errors;

    @Data
    public static class ErrorItem {

        private String sessionId;

        private String errorType;

        private String errorMessage;

        private String errorStack;

        private String pagePath;

        private List<String> userActionBefore;

        private String platform;

        private String deviceModel;

        private String os;

        private String appVersion;

        private Long timestamp;
    }
}
