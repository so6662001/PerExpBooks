package com.qiankubx.module.analytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EventReportDTO {

    @NotEmpty(message = "事件列表不能为空")
    @Valid
    private List<EventItem> events;

    @Data
    public static class EventItem {

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

        private Map<String, Object> extra;

        private Long timestamp;
    }
}
