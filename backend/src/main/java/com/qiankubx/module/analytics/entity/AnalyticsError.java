package com.qiankubx.module.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_analytics_error")
public class AnalyticsError {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String sessionId;

    private String errorType;

    private String errorMessage;

    private String errorStack;

    private String pagePath;

    private String userActionBefore;

    private String platform;

    private String deviceModel;

    private String os;

    private String appVersion;

    private LocalDateTime eventTime;

    private LocalDateTime createdAt;
}
