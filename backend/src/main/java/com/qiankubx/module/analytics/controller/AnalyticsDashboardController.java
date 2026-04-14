package com.qiankubx.module.analytics.controller;

import com.qiankubx.common.response.Result;
import com.qiankubx.module.analytics.service.AnalyticsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsDashboardController {

    private final AnalyticsDashboardService dashboardService;

    @GetMapping("/realtime")
    public Result<Map<String, Object>> realtimeOverview() {
        return Result.ok(dashboardService.getRealtimeOverview());
    }

    @GetMapping("/page-stats")
    public Result<List<Map<String, Object>>> pageStats(@RequestParam String startDate,
                                                       @RequestParam String endDate) {
        return Result.ok(dashboardService.getPageStats(startDate, endDate));
    }

    @GetMapping("/feature-usage")
    public Result<List<Map<String, Object>>> featureUsage(@RequestParam String startDate,
                                                          @RequestParam String endDate) {
        return Result.ok(dashboardService.getFeatureUsage(startDate, endDate));
    }

    @GetMapping("/slow-apis")
    public Result<List<Map<String, Object>>> slowApis(@RequestParam String startDate,
                                                      @RequestParam String endDate) {
        return Result.ok(dashboardService.getSlowApis(startDate, endDate));
    }

    @GetMapping("/errors")
    public Result<List<Map<String, Object>>> errors(@RequestParam String startDate,
                                                     @RequestParam String endDate) {
        return Result.ok(dashboardService.getErrorList(startDate, endDate));
    }
}
