package com.qiankubx.module.stats.service;

import com.qiankubx.module.stats.mapper.StatsMapper;
import com.qiankubx.module.stats.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public StatsOverviewVO getOverview(Long userId) {
        StatsOverviewVO overview = statsMapper.selectOverview(userId);
        if (overview == null) {
            overview = new StatsOverviewVO();
            overview.setTotalExpense(BigDecimal.ZERO);
            overview.setTotalReimbursed(BigDecimal.ZERO);
            overview.setTotalPending(BigDecimal.ZERO);
            overview.setTripCount(0);
            overview.setInvoiceCount(0);
            overview.setTotalTripDays(0);
            return overview;
        }

        Integer totalTripDays = statsMapper.selectTotalTripDays(userId);
        overview.setTotalTripDays(totalTripDays != null ? totalTripDays : 0);

        return overview;
    }

    public List<MonthlyTrendVO> getMonthlyTrend(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return statsMapper.selectMonthlyTrend(userId, year);
    }

    public List<CategoryRatioVO> getCategoryRatio(Long userId, String startDate, String endDate) {
        if (startDate == null || startDate.isBlank()) {
            startDate = LocalDate.now().withDayOfYear(1).toString();
        }
        if (endDate == null || endDate.isBlank()) {
            endDate = LocalDate.now().toString();
        }

        List<CategoryRatioVO> categories = statsMapper.selectCategoryRatio(userId, startDate, endDate);

        BigDecimal total = categories.stream()
                .map(CategoryRatioVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            for (CategoryRatioVO category : categories) {
                BigDecimal ratio = category.getAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(total, 2, RoundingMode.HALF_UP);
                category.setRatio(ratio);
            }
        }

        return categories;
    }

    public TripSummaryVO getTripSummary(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        TripSummaryVO summary = statsMapper.selectTripSummary(userId, year);
        if (summary == null) {
            summary = new TripSummaryVO();
            summary.setTripCount(0);
            summary.setTotalDays(0);
            summary.setTotalSubsidy(BigDecimal.ZERO);
        }

        List<CityStats> cityRanking = statsMapper.selectCityRanking(userId);
        summary.setCityDistribution(cityRanking);

        return summary;
    }

    public List<ReimburseProgressVO> getReimburseProgress(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return statsMapper.selectReimburseProgress(userId, year);
    }
}
