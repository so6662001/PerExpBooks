package com.qiankubx.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyQuotaResetTask {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyQuota() {
        log.info("[定时任务] 重置免费用户月度额度...");
        int updated = jdbcTemplate.update("""
                UPDATE t_user SET monthly_invoice_used = 0, monthly_reimburse_used = 0, updated_at = NOW()
                WHERE (member_status IS NULL OR member_status = 0)
                """);
        log.info("[定时任务] 月度额度重置完成, 更新用户数: {}", updated);
    }
}
