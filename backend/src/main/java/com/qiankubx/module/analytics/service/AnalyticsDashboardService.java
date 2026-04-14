package com.qiankubx.module.analytics.service;

import com.qiankubx.module.analytics.mapper.AnalyticsApiMapper;
import com.qiankubx.module.analytics.mapper.AnalyticsErrorMapper;
import com.qiankubx.module.analytics.mapper.AnalyticsEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private final AnalyticsEventMapper eventMapper;
    private final AnalyticsApiMapper apiMapper;
    private final AnalyticsErrorMapper errorMapper;

    public Map<String, Object> getRealtimeOverview() {
        Long dau = eventMapper.selectTodayDau();
        Long newUsers = eventMapper.selectTodayNewUsers();
        Long paidUsers = eventMapper.selectTodayPaidUsers();

        Map<String, Object> overview = new HashMap<>();
        overview.put("dau", dau != null ? dau : 0L);
        overview.put("newUsers", newUsers != null ? newUsers : 0L);
        overview.put("paidUsers", paidUsers != null ? paidUsers : 0L);
        return overview;
    }

    public List<Map<String, Object>> getPageStats(String startDate, String endDate) {
        return eventMapper.selectPageStats(startDate, endDate);
    }

    public List<Map<String, Object>> getFeatureUsage(String startDate, String endDate) {
        return eventMapper.selectFeatureUsage(startDate, endDate);
    }

    public List<Map<String, Object>> getSlowApis(String startDate, String endDate) {
        return apiMapper.selectSlowApis(startDate, endDate);
    }

    public List<Map<String, Object>> getErrorList(String startDate, String endDate) {
        return errorMapper.selectErrorList(startDate, endDate);
    }
}
