package com.qiankubx.module.analytics.service;

import com.qiankubx.module.analytics.mapper.AnalyticsApiMapper;
import com.qiankubx.module.analytics.mapper.AnalyticsErrorMapper;
import com.qiankubx.module.analytics.mapper.AnalyticsEventMapper;
import com.qiankubx.module.analytics.mapper.AnalyticsPerformanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private final AnalyticsEventMapper eventMapper;
    private final AnalyticsApiMapper apiMapper;
    private final AnalyticsErrorMapper errorMapper;
    private final AnalyticsPerformanceMapper performanceMapper;
    private final JdbcTemplate jdbcTemplate;

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

    public Map<String, Object> getFunnelAnalysis(String type, String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);

        List<Map<String, Object>> steps;
        switch (type) {
            case "register_to_pay" -> {
                steps = jdbcTemplate.queryForList("""
                        SELECT 'register' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'first_open'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        UNION ALL
                        SELECT 'view_plan' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'view_plan'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        UNION ALL
                        SELECT 'purchase' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'purchase'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        """, startDate, endDate, startDate, endDate, startDate, endDate);
            }
            case "reimbursement_flow" -> {
                steps = jdbcTemplate.queryForList("""
                        SELECT 'upload_invoice' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'upload_invoice'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        UNION ALL
                        SELECT 'create_reimburse' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'create_reimburse'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        UNION ALL
                        SELECT 'export_reimburse' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'export_reimburse'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        """, startDate, endDate, startDate, endDate, startDate, endDate);
            }
            case "share_flow" -> {
                steps = jdbcTemplate.queryForList("""
                        SELECT 'view_share_page' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'view_share_page'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        UNION ALL
                        SELECT 'click_share' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'click_share'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        UNION ALL
                        SELECT 'share_complete' AS step, COUNT(DISTINCT user_id) AS cnt
                        FROM t_analytics_event WHERE event_name = 'share_complete'
                            AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                        """, startDate, endDate, startDate, endDate, startDate, endDate);
            }
            default -> steps = new ArrayList<>();
        }
        result.put("steps", steps);
        return result;
    }

    public Map<String, Object> getRetentionAnalysis(String startDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate);

        Long day0 = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM t_analytics_event WHERE DATE(event_time) = ?", Long.class, startDate);

        Long day1 = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT a.user_id) FROM t_analytics_event a
                WHERE DATE(a.event_time) = DATE_ADD(?, INTERVAL 1 DAY)
                AND a.user_id IN (SELECT DISTINCT user_id FROM t_analytics_event WHERE DATE(event_time) = ?)
                """, Long.class, startDate, startDate);

        Long day7 = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT a.user_id) FROM t_analytics_event a
                WHERE DATE(a.event_time) = DATE_ADD(?, INTERVAL 7 DAY)
                AND a.user_id IN (SELECT DISTINCT user_id FROM t_analytics_event WHERE DATE(event_time) = ?)
                """, Long.class, startDate, startDate);

        Long day30 = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT a.user_id) FROM t_analytics_event a
                WHERE DATE(a.event_time) = DATE_ADD(?, INTERVAL 30 DAY)
                AND a.user_id IN (SELECT DISTINCT user_id FROM t_analytics_event WHERE DATE(event_time) = ?)
                """, Long.class, startDate, startDate);

        long base = day0 != null && day0 > 0 ? day0 : 1;
        result.put("day0Users", day0 != null ? day0 : 0);
        result.put("day1Retention", day1 != null ? Math.round(day1 * 100.0 / base) : 0);
        result.put("day7Retention", day7 != null ? Math.round(day7 * 100.0 / base) : 0);
        result.put("day30Retention", day30 != null ? Math.round(day30 * 100.0 / base) : 0);
        return result;
    }

    public List<Map<String, Object>> getPerformanceReport(String startDate, String endDate) {
        return jdbcTemplate.queryForList("""
                SELECT
                    page_path AS pagePath,
                    COUNT(*) AS sampleCount,
                    ROUND(AVG(fcp)) AS avgFcp,
                    ROUND(AVG(lcp)) AS avgLcp,
                    ROUND(AVG(cls), 3) AS avgCls,
                    ROUND(AVG(ttfb)) AS avgTtfb,
                    ROUND(AVG(load_time)) AS avgLoadTime
                FROM t_analytics_performance
                WHERE DATE(event_time) >= ? AND DATE(event_time) <= ?
                GROUP BY page_path
                ORDER BY sampleCount DESC
                """, startDate, endDate);
    }

    public List<Map<String, Object>> getUserSegments() {
        return jdbcTemplate.queryForList("""
                SELECT segment, COUNT(*) AS count FROM (
                    SELECT u.id,
                        CASE
                            WHEN DATEDIFF(NOW(), u.created_at) <= 7 THEN 'new'
                            WHEN u.member_status = 1 AND DATEDIFF(NOW(), u.created_at) <= 90 THEN 'growing'
                            WHEN u.member_status = 1 AND DATEDIFF(NOW(), u.created_at) > 90 THEN 'mature'
                            WHEN DATEDIFF(NOW(), u.updated_at) > 30 AND DATEDIFF(NOW(), u.updated_at) <= 90 THEN 'silent'
                            WHEN DATEDIFF(NOW(), u.updated_at) > 90 THEN 'churned'
                            ELSE 'active'
                        END AS segment
                    FROM t_user u WHERE u.status = 0
                ) t GROUP BY segment ORDER BY count DESC
                """);
    }

    public List<Map<String, Object>> getConversionEffect(String startDate, String endDate) {
        return jdbcTemplate.queryForList("""
                SELECT
                    event_name AS triggerType,
                    COUNT(*) AS showCount,
                    SUM(CASE WHEN extra LIKE '%clicked%' THEN 1 ELSE 0 END) AS clickCount
                FROM t_analytics_event
                WHERE event_name LIKE 'conversion_%'
                    AND DATE(event_time) >= ? AND DATE(event_time) <= ?
                GROUP BY event_name
                ORDER BY showCount DESC
                """, startDate, endDate);
    }

    public List<Map<String, Object>> getTimeDistribution(String startDate, String endDate) {
        return jdbcTemplate.queryForList("""
                SELECT
                    HOUR(event_time) AS hour,
                    COUNT(*) AS eventCount,
                    COUNT(DISTINCT user_id) AS userCount
                FROM t_analytics_event
                WHERE DATE(event_time) >= ? AND DATE(event_time) <= ?
                GROUP BY HOUR(event_time)
                ORDER BY hour
                """, startDate, endDate);
    }

    public List<Map<String, Object>> getPathAnalysis(String startDate, String endDate) {
        return jdbcTemplate.queryForList("""
                SELECT
                    a.page_path AS fromPage,
                    b.page_path AS toPage,
                    COUNT(*) AS transitionCount
                FROM t_analytics_event a
                JOIN t_analytics_event b ON a.session_id = b.session_id
                    AND a.user_id = b.user_id
                    AND b.event_time > a.event_time
                    AND TIMESTAMPDIFF(SECOND, a.event_time, b.event_time) <= 300
                WHERE DATE(a.event_time) >= ? AND DATE(a.event_time) <= ?
                    AND a.page_path IS NOT NULL AND b.page_path IS NOT NULL
                    AND a.page_path != b.page_path
                GROUP BY a.page_path, b.page_path
                ORDER BY transitionCount DESC
                LIMIT 50
                """, startDate, endDate);
    }
}
