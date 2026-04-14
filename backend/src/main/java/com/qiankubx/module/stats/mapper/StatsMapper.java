package com.qiankubx.module.stats.mapper;

import com.qiankubx.module.stats.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatsMapper {

    @Select("""
            SELECT
                COALESCE(SUM(amount), 0) AS totalExpense,
                COALESCE(SUM(CASE WHEN reimburse_status = 2 THEN amount ELSE 0 END), 0) AS totalReimbursed,
                COALESCE(SUM(CASE WHEN reimburse_status = 0 OR reimburse_status = 1 THEN amount ELSE 0 END), 0) AS totalPending,
                COUNT(DISTINCT trip_id) AS tripCount,
                COUNT(CASE WHEN invoice_no IS NOT NULL AND invoice_no != '' THEN 1 END) AS invoiceCount
            FROM t_expense
            WHERE user_id = #{userId} AND status = 0
            """)
    StatsOverviewVO selectOverview(@Param("userId") Long userId);

    @Select("""
            SELECT
                COALESCE(SUM(DATEDIFF(
                    COALESCE(t.end_date, t.start_date),
                    t.start_date
                ) + 1), 0) AS totalTripDays
            FROM t_trip t
            WHERE t.user_id = #{userId} AND t.status = 0
            """)
    Integer selectTotalTripDays(@Param("userId") Long userId);

    @Select("""
            SELECT
                DATE_FORMAT(expense_date, '%Y-%m') AS month,
                COALESCE(SUM(amount), 0) AS amount
            FROM t_expense
            WHERE user_id = #{userId} AND status = 0
                AND YEAR(expense_date) = #{year}
            GROUP BY DATE_FORMAT(expense_date, '%Y-%m')
            ORDER BY month
            """)
    List<MonthlyTrendVO> selectMonthlyTrend(@Param("userId") Long userId, @Param("year") Integer year);

    @Select("""
            SELECT
                e.category_id AS categoryId,
                COALESCE(c.name, e.invoice_type, '其他') AS categoryName,
                COALESCE(SUM(e.amount), 0) AS amount
            FROM t_expense e
            LEFT JOIN t_category c ON e.category_id = c.id
            WHERE e.user_id = #{userId} AND e.status = 0
                AND e.expense_date >= #{startDate}
                AND e.expense_date <= #{endDate}
            GROUP BY e.category_id, categoryName
            ORDER BY amount DESC
            """)
    List<CategoryRatioVO> selectCategoryRatio(@Param("userId") Long userId,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    @Select("""
            SELECT
                COUNT(*) AS tripCount,
                COALESCE(SUM(DATEDIFF(
                    COALESCE(end_date, start_date),
                    start_date
                ) + 1), 0) AS totalDays,
                COALESCE(SUM(subsidy), 0) AS totalSubsidy
            FROM t_trip
            WHERE user_id = #{userId} AND status = 0
                AND YEAR(start_date) = #{year}
            """)
    TripSummaryVO selectTripSummary(@Param("userId") Long userId, @Param("year") Integer year);

    @Select("""
            SELECT
                city,
                COUNT(*) AS count,
                COALESCE(SUM(subsidy), 0) AS amount
            FROM t_trip
            WHERE user_id = #{userId} AND status = 0
                AND city IS NOT NULL AND city != ''
            GROUP BY city
            ORDER BY count DESC
            """)
    List<CityStats> selectCityRanking(@Param("userId") Long userId);

    @Select("""
            SELECT
                DATE_FORMAT(expense_date, '%m') AS month,
                COALESCE(SUM(amount), 0) AS amount
            FROM t_expense
            WHERE user_id = #{userId} AND status = 0
                AND YEAR(expense_date) = #{year}
            GROUP BY DATE_FORMAT(expense_date, '%m')
            ORDER BY month
            """)
    List<MonthlyTrendVO> selectYearMonthlyExpense(@Param("userId") Long userId, @Param("year") Integer year);

    @Select("""
            SELECT
                bt.destination AS city,
                COUNT(DISTINCT bt.id) AS count,
                COALESCE(SUM(e.amount), 0) AS amount
            FROM t_expense e
            JOIN t_business_trip bt ON e.trip_id = bt.id
            WHERE e.user_id = #{userId} AND e.status = 0
                AND bt.destination IS NOT NULL AND bt.destination != ''
                AND YEAR(e.expense_date) = #{year}
            GROUP BY bt.destination
            ORDER BY amount DESC
            """)
    List<CityStats> selectCityExpenseRanking(@Param("userId") Long userId, @Param("year") Integer year);

    @Select("""
            SELECT
                e.expense_date AS expenseDate,
                COALESCE(ec.name, e.invoice_type, '其他') AS categoryName,
                e.amount,
                e.tax_amount AS taxAmount,
                e.invoice_no AS invoiceNo,
                e.invoice_type AS invoiceType,
                e.seller_name AS sellerName,
                e.description,
                e.reimburse_status AS reimburseStatus
            FROM t_expense e
            LEFT JOIN t_expense_category ec ON e.category_id = ec.id
            WHERE e.user_id = #{userId} AND e.status = 0
                AND e.expense_date >= #{startDate}
                AND e.expense_date <= #{endDate}
            ORDER BY e.expense_date DESC
            """)
    List<java.util.Map<String, Object>> selectExpenseForExport(@Param("userId") Long userId,
                                                               @Param("startDate") String startDate,
                                                               @Param("endDate") String endDate);

    @Select("""
            SELECT
                DATE_FORMAT(r.created_at, '%Y-%m') AS month,
                COALESCE(SUM(r.total_amount), 0) AS submitted,
                COALESCE(SUM(CASE WHEN r.reimburse_status = 2 THEN r.total_amount ELSE 0 END), 0) AS received,
                COALESCE(SUM(CASE WHEN r.reimburse_status != 2 THEN r.total_amount ELSE 0 END), 0) AS pending
            FROM t_reimbursement r
            WHERE r.user_id = #{userId} AND r.status = 0
                AND YEAR(r.created_at) = #{year}
            GROUP BY DATE_FORMAT(r.created_at, '%Y-%m')
            ORDER BY month
            """)
    List<ReimburseProgressVO> selectReimburseProgress(@Param("userId") Long userId, @Param("year") Integer year);
}
