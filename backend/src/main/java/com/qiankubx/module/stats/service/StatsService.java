package com.qiankubx.module.stats.service;

import com.alibaba.excel.EasyExcel;
import com.qiankubx.module.stats.mapper.StatsMapper;
import com.qiankubx.module.stats.vo.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public StatsOverviewVO getOverview(Long userId, Integer year) {
        StatsOverviewVO overview;
        Integer totalTripDays;

        if (year != null) {
            overview = statsMapper.selectOverviewByYear(userId, year);
            totalTripDays = statsMapper.selectTotalTripDaysByYear(userId, year);
        } else {
            overview = statsMapper.selectOverview(userId);
            totalTripDays = statsMapper.selectTotalTripDays(userId);
        }

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

        List<CityStats> cityRanking = statsMapper.selectCityRanking(userId, year);
        summary.setCityDistribution(cityRanking);

        return summary;
    }

    public List<ReimburseProgressVO> getReimburseProgress(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return statsMapper.selectReimburseProgress(userId, year);
    }

    public List<YearlyCompareVO> getYearlyCompare(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        List<MonthlyTrendVO> currentYearData = statsMapper.selectYearMonthlyExpense(userId, year);
        List<MonthlyTrendVO> lastYearData = statsMapper.selectYearMonthlyExpense(userId, year - 1);

        Map<String, BigDecimal> lastYearMap = new LinkedHashMap<>();
        for (MonthlyTrendVO item : lastYearData) {
            lastYearMap.put(item.getMonth(), item.getAmount());
        }

        Map<String, BigDecimal> currentYearMap = new LinkedHashMap<>();
        for (MonthlyTrendVO item : currentYearData) {
            currentYearMap.put(item.getMonth(), item.getAmount());
        }

        List<YearlyCompareVO> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String month = String.format("%02d", m);
            YearlyCompareVO vo = new YearlyCompareVO();
            vo.setMonth(month);
            vo.setCurrentYear(currentYearMap.getOrDefault(month, BigDecimal.ZERO));
            vo.setLastYear(lastYearMap.getOrDefault(month, BigDecimal.ZERO));
            result.add(vo);
        }
        return result;
    }

    public List<CityStats> getCityRankingByYear(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return statsMapper.selectCityExpenseRanking(userId, year);
    }

    public List<CalendarDayVO> getExpenseCalendar(Long userId, int year, int month) {
        return statsMapper.selectExpenseCalendar(userId, year, month);
    }

    public void exportExcel(Long userId, String startDate, String endDate, Long categoryId, HttpServletResponse response) {
        if (startDate == null || startDate.isBlank()) {
            startDate = LocalDate.now().withDayOfYear(1).toString();
        }
        if (endDate == null || endDate.isBlank()) {
            endDate = LocalDate.now().toString();
        }

        List<Map<String, Object>> rows;
        if (categoryId != null) {
            rows = statsMapper.selectExpenseForExportWithCategory(userId, startDate, endDate, categoryId);
        } else {
            rows = statsMapper.selectExpenseForExport(userId, startDate, endDate);
        }

        List<ExpenseExcelVO> excelData = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ExpenseExcelVO vo = new ExpenseExcelVO();
            vo.setExpenseDate(row.get("expenseDate") != null ? row.get("expenseDate").toString() : "");
            vo.setCategoryName((String) row.get("categoryName"));
            vo.setAmount(row.get("amount") != null ? new BigDecimal(row.get("amount").toString()) : null);
            vo.setTaxAmount(row.get("taxAmount") != null ? new BigDecimal(row.get("taxAmount").toString()) : null);
            vo.setInvoiceNo((String) row.get("invoiceNo"));
            vo.setInvoiceType((String) row.get("invoiceType"));
            vo.setSellerName((String) row.get("sellerName"));
            vo.setDescription((String) row.get("description"));
            Object status = row.get("reimburseStatus");
            vo.setReimburseStatusName(getReimburseStatusName(status != null ? ((Number) status).intValue() : 0));
            excelData.add(vo);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("费用明细_" + startDate + "_" + endDate, StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), ExpenseExcelVO.class).sheet("费用明细").doWrite(excelData);
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    private String getReimburseStatusName(int status) {
        return switch (status) {
            case 0 -> "未报销";
            case 1 -> "报销中";
            case 2 -> "已报销";
            default -> "未知";
        };
    }
}
