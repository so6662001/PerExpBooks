package com.qiankubx.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireTask {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 2 * * ?")
    public void expireCoupons() {
        log.info("[定时任务] 检查过期优惠券...");
        int updated = jdbcTemplate.update("""
                UPDATE t_user_coupon SET use_status = 2
                WHERE use_status = 0 AND expire_at < NOW()
                """);
        log.info("[定时任务] 过期优惠券处理完成, 更新数量: {}", updated);
    }
}
