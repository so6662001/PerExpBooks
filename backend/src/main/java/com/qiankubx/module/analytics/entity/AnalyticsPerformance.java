package com.qiankubx.module.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_analytics_performance")
public class AnalyticsPerformance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

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

    private LocalDateTime eventTime;

    private LocalDateTime createdAt;
}
