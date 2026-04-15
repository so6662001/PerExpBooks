package com.qiankubx.module.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.security.ChainHashService;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.promotion.entity.PointsLog;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.mapper.PointsLogMapper;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final JdbcTemplate jdbcTemplate;
    private final PromoterLevelMapper promoterLevelMapper;
    private final PointsLogMapper pointsLogMapper;
    private final DataSignService dataSignService;
    private final ChainHashService chainHashService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String RECONCILE_REPORT_KEY = "audit:reconcile:report";

    public Map<String, Object> queryLogs(String targetTable, Long targetId, Long operatorId,
                                          String startDate, String endDate, Integer page, Integer pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM t_audit_log WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM t_audit_log WHERE 1=1");
        List<Object> params = new ArrayList<>();
        List<Object> countParams = new ArrayList<>();

        if (targetTable != null && !targetTable.isBlank()) {
            sql.append(" AND target_table = ?");
            countSql.append(" AND target_table = ?");
            params.add(targetTable);
            countParams.add(targetTable);
        }
        if (targetId != null) {
            sql.append(" AND target_id = ?");
            countSql.append(" AND target_id = ?");
            params.add(targetId);
            countParams.add(targetId);
        }
        if (operatorId != null) {
            sql.append(" AND operator_id = ?");
            countSql.append(" AND operator_id = ?");
            params.add(operatorId);
            countParams.add(operatorId);
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND created_at >= ?");
            countSql.append(" AND created_at >= ?");
            params.add(startDate);
            countParams.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND created_at <= ?");
            countSql.append(" AND created_at <= ?");
            params.add(endDate + " 23:59:59");
            countParams.add(endDate + " 23:59:59");
        }

        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countParams.toArray());

        int offset = (page - 1) * pageSize;
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("records", records);
        return result;
    }

    public Map<String, Object> reconcileAll() {
        log.info("开始全量对账...");
        List<PromoterLevel> allLevels = promoterLevelMapper.selectList(null);
        int totalChecked = 0;
        int pointsMismatch = 0;
        int balanceMismatch = 0;
        List<Map<String, Object>> anomalies = new ArrayList<>();

        for (PromoterLevel level : allLevels) {
            totalChecked++;
            Long userId = level.getUserId();

            Integer pointsSum = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(points), 0) FROM t_points_log WHERE user_id = ?",
                    Integer.class, userId);

            if (pointsSum == null) pointsSum = 0;
            if (!pointsSum.equals(level.getPoints())) {
                pointsMismatch++;
                Map<String, Object> anomaly = new LinkedHashMap<>();
                anomaly.put("userId", userId);
                anomaly.put("type", "points_mismatch");
                anomaly.put("expected", pointsSum);
                anomaly.put("actual", level.getPoints());
                anomalies.add(anomaly);
                log.warn("积分不匹配: userId={}, expected={}, actual={}", userId, pointsSum, level.getPoints());
            }

            // available_balance == SUM(已结算返佣) - SUM(已完成提现)
            Map<String, Object> balanceCheck = jdbcTemplate.queryForMap(
                    """
                    SELECT
                        COALESCE((SELECT SUM(commission_amount) FROM t_commission WHERE user_id = ? AND status = 1), 0) AS settled,
                        COALESCE((SELECT SUM(amount) FROM t_withdrawal WHERE user_id = ? AND status = 1), 0) AS withdrawn
                    """, userId, userId);
            java.math.BigDecimal settled = (java.math.BigDecimal) balanceCheck.get("settled");
            java.math.BigDecimal withdrawn = (java.math.BigDecimal) balanceCheck.get("withdrawn");
            java.math.BigDecimal expectedBalance = settled.subtract(withdrawn);

            if (expectedBalance.compareTo(level.getAvailableBalance()) != 0) {
                balanceMismatch++;
                Map<String, Object> anomaly = new LinkedHashMap<>();
                anomaly.put("userId", userId);
                anomaly.put("type", "balance_mismatch");
                anomaly.put("expectedBalance", expectedBalance);
                anomaly.put("actualBalance", level.getAvailableBalance());
                anomaly.put("settled", settled);
                anomaly.put("withdrawn", withdrawn);
                anomalies.add(anomaly);
                log.warn("余额不匹配: userId={}, expected={}, actual={}", userId, expectedBalance, level.getAvailableBalance());
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("checkTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        report.put("totalChecked", totalChecked);
        report.put("pointsMismatch", pointsMismatch);
        report.put("balanceMismatch", balanceMismatch);
        report.put("anomalies", anomalies);
        report.put("status", (pointsMismatch + balanceMismatch) == 0 ? "PASS" : "FAIL");

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules();
            stringRedisTemplate.opsForValue().set(RECONCILE_REPORT_KEY, mapper.writeValueAsString(report), 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("对账报告缓存失败", e);
        }

        log.info("全量对账完成: totalChecked={}, pointsMismatch={}, balanceMismatch={}", totalChecked, pointsMismatch, balanceMismatch);
        return report;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getReconcileReport() {
        String json = stringRedisTemplate.opsForValue().get(RECONCILE_REPORT_KEY);
        if (json == null || json.isBlank()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("message", "暂无对账报告，请先执行对账");
            return empty;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("解析对账报告失败", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("message", "解析失败");
            return error;
        }
    }

    public List<Map<String, Object>> getTamperAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        List<Map<String, Object>> expenses = jdbcTemplate.queryForList(
                "SELECT id, user_id, amount, reimburse_status, created_at, data_sign FROM t_expense WHERE data_sign IS NOT NULL LIMIT 100");
        for (Map<String, Object> row : expenses) {
            String payload = row.get("id") + "|" + row.get("user_id") + "|" + row.get("amount")
                    + "|" + row.get("reimburse_status") + "|" + row.get("created_at");
            String storedSign = (String) row.get("data_sign");
            if (storedSign != null && !dataSignService.verify(payload, storedSign)) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("table", "t_expense");
                alert.put("id", row.get("id"));
                alert.put("storedSign", storedSign);
                alerts.add(alert);
            }
        }
        return alerts;
    }

    public Map<String, Object> verifySign(String table, Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", table);
        result.put("id", id);

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM " + sanitizeTable(table) + " WHERE id = ?", id);
            String storedSign = (String) row.get("data_sign");
            if (storedSign == null) {
                result.put("verified", false);
                result.put("message", "记录无签名");
                return result;
            }

            String payload = buildPayloadForTable(table, row);
            boolean valid = dataSignService.verify(payload, storedSign);
            result.put("verified", valid);
            result.put("message", valid ? "签名校验通过" : "签名校验失败，数据可能被篡改");
        } catch (Exception e) {
            result.put("verified", false);
            result.put("message", "校验失败: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> verifyChain(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);

        List<PointsLog> logs = pointsLogMapper.selectList(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .orderByAsc(PointsLog::getId)
        );

        if (logs.isEmpty()) {
            result.put("verified", true);
            result.put("message", "无积分流水");
            result.put("totalRecords", 0);
            return result;
        }

        int verified = 0;
        int failed = 0;
        Long firstFailId = null;

        for (PointsLog logEntry : logs) {
            String prevHash = logEntry.getPrevHash() != null ? logEntry.getPrevHash() : "GENESIS";
            String chainPayload = logEntry.getId() + "|" + logEntry.getUserId() + "|" + logEntry.getPoints() + "|"
                    + logEntry.getBalanceAfter() + "|" + logEntry.getAction() + "|" + prevHash + "|" + logEntry.getCreatedAt();
            boolean chainValid = chainHashService.verifyHash(chainPayload, logEntry.getChainHash());
            if (chainValid) {
                verified++;
            } else {
                failed++;
                if (firstFailId == null) firstFailId = logEntry.getId();
            }
        }

        result.put("totalRecords", logs.size());
        result.put("verified", failed == 0);
        result.put("verifiedCount", verified);
        result.put("failedCount", failed);
        if (firstFailId != null) {
            result.put("firstFailId", firstFailId);
        }
        result.put("message", failed == 0 ? "链式哈希完整性校验通过" : "链式哈希断裂，数据可能被篡改");
        return result;
    }

    public List<Map<String, Object>> getAnomalyUsers() {
        String sql = """
                SELECT
                    pl.user_id AS userId,
                    pl.points AS currentPoints,
                    COALESCE((SELECT SUM(points) FROM t_points_log WHERE user_id = pl.user_id), 0) AS computedPoints,
                    pl.available_balance AS currentBalance,
                    COALESCE((SELECT SUM(commission_amount) FROM t_commission WHERE user_id = pl.user_id AND status = 1), 0) AS settledCommission,
                    COALESCE((SELECT SUM(amount) FROM t_withdrawal WHERE user_id = pl.user_id AND status = 1), 0) AS completedWithdrawal
                FROM t_promoter_level pl
                HAVING currentPoints != computedPoints
                    OR currentBalance != (settledCommission - completedWithdrawal)
                """;
        return jdbcTemplate.queryForList(sql);
    }

    private String sanitizeTable(String table) {
        if (!table.matches("^t_[a-z_]+$")) {
            throw new IllegalArgumentException("非法表名");
        }
        return table;
    }

    private String buildPayloadForTable(String table, Map<String, Object> row) {
        return switch (table) {
            case "t_expense" -> row.get("id") + "|" + row.get("user_id") + "|" + row.get("amount")
                    + "|" + row.get("reimburse_status") + "|" + row.get("created_at");
            case "t_member_order" -> row.get("id") + "|" + row.get("user_id") + "|" + row.get("pay_amount")
                    + "|" + row.get("plan_type") + "|" + row.get("pay_status") + "|" + row.get("pay_time")
                    + "|" + row.get("created_at");
            case "t_commission" -> row.get("id") + "|" + row.get("user_id") + "|" + row.get("commission_amount")
                    + "|" + row.get("commission_type") + "|" + row.get("order_id") + "|" + row.get("invitee_id")
                    + "|" + row.get("status") + "|" + row.get("created_at");
            case "t_withdrawal" -> row.get("id") + "|" + row.get("user_id") + "|" + row.get("amount")
                    + "|" + row.get("status") + "|" + row.get("created_at");
            case "t_points_log" -> row.get("user_id") + "|" + row.get("points") + "|" + row.get("balance_after")
                    + "|" + row.get("action") + "|" + row.get("created_at");
            case "t_user_coupon" -> row.get("id") + "|" + row.get("user_id") + "|" + row.get("discount_value")
                    + "|" + row.get("use_status") + "|" + row.get("created_at");
            case "t_promoter_level" -> row.get("user_id") + "|" + row.get("points") + "|" + row.get("total_points")
                    + "|" + row.get("available_balance") + "|" + row.get("frozen_balance") + "|" + row.get("withdrawn_amount")
                    + "|" + row.get("total_commission") + "|" + row.get("level") + "|" + row.get("updated_at");
            default -> throw new IllegalArgumentException("不支持校验的表: " + table);
        };
    }
}
