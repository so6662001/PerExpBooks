package com.qiankubx.module.analytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PerformanceReportDTO {

    @NotEmpty(message = "性能数据列表不能为空")
    @Valid
    private List<PerformanceItem> performances;

    @Data
    public static class PerformanceItem {

        private String sessionId;

        private String pagePath;

        private String platform;

        private Integer fcp;

        private Integer lcp;

        private Integer fid;

        private BigDecimal cls;

        private Integer ttfb;

        private Integer loadTime;

        private Integer inp;

        private Integer domReady;

        private Integer resourceCount;

        private Integer resourceSizeKb;

        private Integer jsHeapSizeMb;

        private String networkType;

        private Long timestamp;
    }
}
