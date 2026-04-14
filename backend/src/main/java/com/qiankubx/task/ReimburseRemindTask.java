package com.qiankubx.task;

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
public class ReimburseRemindTask {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 9 ? * MON")
    public void remindPendingReimburse() {
        log.info("[定时任务] 提醒待报销费用...");
        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT user_id, COUNT(*) AS pending_count, COALESCE(SUM(amount), 0) AS pending_amount
                FROM t_expense
                WHERE status = 0 AND reimburse_status = 0
                GROUP BY user_id
                HAVING pending_count > 0
                """);

        for (Map<String, Object> row : users) {
            log.info("[定时任务] 报销提醒: userId={}, 待报销{}笔, 金额={}",
                    row.get("user_id"), row.get("pending_count"), row.get("pending_amount"));
        }
        log.info("[定时任务] 报销提醒完成, 通知用户数: {}", users.size());
    }
}
