package com.qiankubx.module.stats.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.stats.service.StatsService;
import com.qiankubx.module.stats.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<StatsOverviewVO> getOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getOverview(userId));
    }

    @GetMapping("/monthly-trend")
    public Result<List<MonthlyTrendVO>> getMonthlyTrend(HttpServletRequest request,
                                                        @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getMonthlyTrend(userId, year));
    }

    @GetMapping("/category-ratio")
    public Result<List<CategoryRatioVO>> getCategoryRatio(HttpServletRequest request,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getCategoryRatio(userId, startDate, endDate));
    }

    @GetMapping("/trip-summary")
    public Result<TripSummaryVO> getTripSummary(HttpServletRequest request,
                                                @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getTripSummary(userId, year));
    }

    @GetMapping("/reimburse-progress")
    public Result<List<ReimburseProgressVO>> getReimburseProgress(HttpServletRequest request,
                                                                  @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getReimburseProgress(userId, year));
    }

    @GetMapping("/yearly-compare")
    public Result<List<YearlyCompareVO>> getYearlyCompare(HttpServletRequest request,
                                                           @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getYearlyCompare(userId, year));
    }

    @GetMapping("/city-ranking")
    public Result<List<CityStats>> getCityRanking(HttpServletRequest request,
                                                   @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getCityRankingByYear(userId, year));
    }

    @GetMapping("/export")
    public void exportExcel(HttpServletRequest request, HttpServletResponse response,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        statsService.exportExcel(userId, startDate, endDate, response);
    }
}
