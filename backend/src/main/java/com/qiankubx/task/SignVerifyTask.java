package com.qiankubx.task;

import com.qiankubx.common.security.DataSignService;
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

    @Scheduled(cron = "0 0 4 * * ?")
    public void spotCheckSigns() {
        log.info("[定时任务] 抽检行级签名...");
        int checked = 0;
        int failed = 0;

        List<Map<String, Object>> expenses = jdbcTemplate.queryForList(
                "SELECT id, user_id, amount, type, expense_date, invoice_no, data_sign FROM t_expense WHERE data_sign IS NOT NULL ORDER BY RAND() LIMIT 100");
        for (Map<String, Object> row : expenses) {
            checked++;
            String payload = row.get("id") + "|" + row.get("user_id") + "|" + row.get("amount")
                    + "|" + row.get("type") + "|" + row.get("expense_date") + "|" + row.get("invoice_no");
            String storedSign = (String) row.get("data_sign");
            if (!dataSignService.verify(payload, storedSign)) {
                failed++;
                log.warn("[定时任务] 签名校验失败: table=t_expense, id={}", row.get("id"));
            }
        }

        log.info("[定时任务] 签名抽检完成, 检查: {}, 失败: {}", checked, failed);
    }
}
