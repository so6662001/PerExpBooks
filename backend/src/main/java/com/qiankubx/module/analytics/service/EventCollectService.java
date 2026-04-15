package com.qiankubx.module.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiankubx.module.analytics.dto.ErrorReportDTO;
import com.qiankubx.module.analytics.dto.EventReportDTO;
import com.qiankubx.module.analytics.dto.PerformanceReportDTO;
import com.qiankubx.module.analytics.entity.AnalyticsError;
import com.qiankubx.module.analytics.entity.AnalyticsEvent;
import com.qiankubx.module.analytics.entity.AnalyticsPerformance;
import com.qiankubx.module.analytics.mapper.AnalyticsErrorMapper;
import com.qiankubx.module.analytics.mapper.AnalyticsEventMapper;
import com.qiankubx.module.analytics.mapper.AnalyticsPerformanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCollectService {

    private final AnalyticsEventMapper eventMapper;
    private final AnalyticsPerformanceMapper performanceMapper;
    private final AnalyticsErrorMapper errorMapper;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void reportEvents(Long userId, EventReportDTO dto) {
        for (EventReportDTO.EventItem item : dto.getEvents()) {
            AnalyticsEvent event = new AnalyticsEvent();
            event.setUserId(userId);
            event.setEventId(item.getEventId());
            event.setEventType(item.getEventType());
            event.setEventName(item.getEventName());
            event.setSessionId(item.getSessionId());
            event.setPagePath(item.getPagePath());
            event.setPageTitle(item.getPageTitle());
            event.setReferrerPath(item.getReferrerPath());
            event.setPlatform(item.getPlatform());
            event.setDeviceModel(item.getDeviceModel());
            event.setOs(item.getOs());
            event.setOsVersion(item.getOsVersion());
            event.setScreenWidth(item.getScreenWidth());
            event.setScreenHeight(item.getScreenHeight());
            event.setNetworkType(item.getNetworkType());
            event.setAppVersion(item.getAppVersion());
            event.setMemberType(item.getMemberType());
            event.setEventTime(toLocalDateTime(item.getTimestamp()));
            event.setCreatedAt(LocalDateTime.now());

            if (item.getExtra() != null) {
                try {
                    event.setExtra(objectMapper.writeValueAsString(item.getExtra()));
                } catch (JsonProcessingException e) {
                    log.warn("序列化extra字段失败: {}", e.getMessage());
                    event.setExtra("{}");
                }
            }

            eventMapper.insert(event);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reportPerformance(Long userId, PerformanceReportDTO dto) {
        for (PerformanceReportDTO.PerformanceItem item : dto.getPerformances()) {
            AnalyticsPerformance perf = new AnalyticsPerformance();
            perf.setUserId(userId);
            perf.setSessionId(item.getSessionId());
            perf.setPagePath(item.getPagePath());
            perf.setPlatform(item.getPlatform());
            perf.setFcp(item.getFcp());
            perf.setLcp(item.getLcp());
            perf.setFid(item.getFid());
            perf.setCls(item.getCls());
            perf.setTtfb(item.getTtfb());
            perf.setLoadTime(item.getLoadTime());
            perf.setInp(item.getInp());
            perf.setDomReady(item.getDomReady());
            perf.setResourceCount(item.getResourceCount());
            perf.setResourceSizeKb(item.getResourceSizeKb());
            perf.setJsHeapSizeMb(item.getJsHeapSizeMb());
            perf.setNetworkType(item.getNetworkType());
            perf.setEventTime(toLocalDateTime(item.getTimestamp()));
            perf.setCreatedAt(LocalDateTime.now());

            performanceMapper.insert(perf);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reportErrors(Long userId, ErrorReportDTO dto) {
        for (ErrorReportDTO.ErrorItem item : dto.getErrors()) {
            AnalyticsError error = new AnalyticsError();
            error.setUserId(userId);
            error.setSessionId(item.getSessionId());
            error.setErrorType(item.getErrorType());
            error.setErrorMessage(item.getErrorMessage());
            error.setErrorStack(item.getErrorStack());
            error.setPagePath(item.getPagePath());
            error.setPlatform(item.getPlatform());
            error.setDeviceModel(item.getDeviceModel());
            error.setOs(item.getOs());
            error.setAppVersion(item.getAppVersion());
            error.setEventTime(toLocalDateTime(item.getTimestamp()));
            error.setCreatedAt(LocalDateTime.now());

            if (item.getUserActionBefore() != null) {
                try {
                    error.setUserActionBefore(objectMapper.writeValueAsString(item.getUserActionBefore()));
                } catch (JsonProcessingException e) {
                    log.warn("序列化userActionBefore字段失败: {}", e.getMessage());
                    error.setUserActionBefore("[]");
                }
            }

            errorMapper.insert(error);
        }
    }

    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
