package com.qiankubx.task;

import com.qiankubx.common.security.DataSignService;
import com.qiankubx.common.security.TamperAlertService;
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
public class SignVerifyTask {

    private final JdbcTemplate jdbcTemplate;
    private final DataSignService dataSignService;
    private final TamperAlertService tamperAlertService;

    @Scheduled(cron = "0 0 4 * * ?")
    public void spotCheckSigns() {
        log.info("[定时任务] 抽检行级签名...");
        int checked = 0;
        int failed = 0;

        List<Map<String, Object>> expenses = jdbcTemplate.queryForList(
                "SELECT id, user_id, amount, reimburse_status, created_at, data_sign FROM t_expense WHERE data_sign IS NOT NULL ORDER BY RAND() LIMIT 100");
        for (Map<String, Object> row : expenses) {
            checked++;
            String payload = row.get("id") + "|" + row.get("user_id") + "|" + row.get("amount")
                    + "|" + row.get("reimburse_status") + "|" + row.get("created_at");
            String storedSign = (String) row.get("data_sign");
            if (!dataSignService.verify(payload, storedSign)) {
                failed++;
                Long userId = ((Number) row.get("user_id")).longValue();
                log.warn("[定时任务] 签名校验失败: table=t_expense, id={}", row.get("id"));
                tamperAlertService.alertUrgent("sign_verify_fail",
                        "费用记录签名校验失败, id=" + row.get("id"), userId);
            }
        }

        log.info("[定时任务] 签名抽检完成, 检查: {}, 失败: {}", checked, failed);
    }
}
