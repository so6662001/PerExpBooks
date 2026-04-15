package com.qiankubx.module.stats.controller;

import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.module.stats.service.StatsService;
import com.qiankubx.module.stats.vo.*;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
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
    private final UserMapper userMapper;

    private void checkStatsPermission(Long userId) {
        User user = userMapper.selectById(userId);
        if (user.getMemberStatus() == null || user.getMemberStatus() == 0) {
            throw new BizException(ResultCode.MEMBER_REQUIRED.getCode(), "统计报表为会员专属功能，请升级会员查看");
        }
    }

    @GetMapping("/overview")
    public Result<StatsOverviewVO> getOverview(
            @RequestParam(required = false) Integer year,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(statsService.getOverview(userId, year));
    }

    @GetMapping("/monthly-trend")
    public Result<List<MonthlyTrendVO>> getMonthlyTrend(HttpServletRequest request,
                                                        @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        return Result.ok(statsService.getMonthlyTrend(userId, year));
    }

    @GetMapping("/category-ratio")
    public Result<List<CategoryRatioVO>> getCategoryRatio(HttpServletRequest request,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        return Result.ok(statsService.getCategoryRatio(userId, startDate, endDate));
    }

    @GetMapping("/trip-summary")
    public Result<TripSummaryVO> getTripSummary(HttpServletRequest request,
                                                @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        return Result.ok(statsService.getTripSummary(userId, year));
    }

    @GetMapping("/reimburse-progress")
    public Result<List<ReimburseProgressVO>> getReimburseProgress(HttpServletRequest request,
                                                                  @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        return Result.ok(statsService.getReimburseProgress(userId, year));
    }

    @GetMapping("/yearly-compare")
    public Result<List<YearlyCompareVO>> getYearlyCompare(HttpServletRequest request,
                                                           @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        return Result.ok(statsService.getYearlyCompare(userId, year));
    }

    @GetMapping("/city-ranking")
    public Result<List<CityStats>> getCityRanking(HttpServletRequest request,
                                                   @RequestParam(required = false) Integer year) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        return Result.ok(statsService.getCityRankingByYear(userId, year));
    }

    @GetMapping("/calendar")
    public Result<List<CalendarDayVO>> getExpenseCalendar(
            @RequestParam int year, @RequestParam int month,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        return Result.ok(statsService.getExpenseCalendar(userId, year, month));
    }

    @GetMapping("/export")
    public void exportExcel(HttpServletRequest request, HttpServletResponse response,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate,
                            @RequestParam(required = false) Long categoryId) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        checkStatsPermission(userId);
        statsService.exportExcel(userId, startDate, endDate, categoryId, response);
    }
}
