package com.qiankubx.module.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_analytics_api")
public class AnalyticsApi {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String sessionId;

    private String apiPath;

    private String method;

    private Integer statusCode;

    private Integer durationMs;

    private Integer requestSizeKb;

    private Integer responseSizeKb;

    private Integer isTimeout;

    private String errorType;

    private String pagePath;

    private LocalDateTime eventTime;

    private LocalDateTime createdAt;
}
