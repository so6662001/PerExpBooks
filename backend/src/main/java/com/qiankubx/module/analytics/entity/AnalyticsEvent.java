package com.qiankubx.module.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_analytics_event")
public class AnalyticsEvent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String eventId;

    private String eventType;

    private String eventName;

    private Long userId;

    private String sessionId;

    private String pagePath;

    private String pageTitle;

    private String referrerPath;

    private String platform;

    private String deviceModel;

    private String os;

    private String osVersion;

    private Integer screenWidth;

    private Integer screenHeight;

    private String networkType;

    private String appVersion;

    private String memberType;

    private String extra;

    private LocalDateTime eventTime;

    private LocalDateTime createdAt;
}
