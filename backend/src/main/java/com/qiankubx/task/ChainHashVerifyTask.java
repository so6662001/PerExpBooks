package com.qiankubx.task;

import com.qiankubx.common.security.TamperAlertService;
import com.qiankubx.module.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChainHashVerifyTask {

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;
    private final TamperAlertService tamperAlertService;

    @Scheduled(cron = "0 30 4 * * ?")
    public void spotCheckChainHash() {
        log.info("[定时任务] 抽检链式哈希完整性...");

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT DISTINCT user_id FROM t_points_log ORDER BY RAND() LIMIT 10");

        int checked = 0;
        int failed = 0;
        for (Map<String, Object> row : users) {
            Long userId = ((Number) row.get("user_id")).longValue();
            checked++;
            try {
                Map<String, Object> result = auditService.verifyChain(userId);
                if (!Boolean.TRUE.equals(result.get("verified"))) {
                    failed++;
                    log.warn("[定时任务] 链式哈希校验失败: userId={}", userId);
                    tamperAlertService.alertUrgent("chain_hash_fail",
                            "积分流水链式哈希校验失败", userId);
                }
            } catch (Exception e) {
                failed++;
                log.error("[定时任务] 链式哈希校验异常: userId={}", userId, e);
                tamperAlertService.alertWarning("chain_hash_error",
                        "链式哈希校验异常: " + e.getMessage(), userId);
            }
        }

        log.info("[定时任务] 链式哈希抽检完成, 检查用户: {}, 失败: {}", checked, failed);
    }
}
