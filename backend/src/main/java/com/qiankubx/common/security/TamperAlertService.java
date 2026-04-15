package com.qiankubx.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TamperAlertService {

    private final StringRedisTemplate redisTemplate;

    public void alertUrgent(String type, String message, Long userId) {
        log.error("[紧急安全告警] type={}, userId={}, msg={}", type, userId, message);
        String alertJson = String.format(
                "{\"type\":\"%s\",\"userId\":%d,\"message\":\"%s\",\"time\":\"%s\",\"level\":\"urgent\"}",
                type, userId, message, LocalDateTime.now());
        redisTemplate.opsForList().leftPush("security:tamper_alerts", alertJson);
        redisTemplate.opsForList().trim("security:tamper_alerts", 0, 999);

        redisTemplate.opsForValue().set("security:frozen:" + userId, "1", 24, TimeUnit.HOURS);
    }

    public void alertWarning(String type, String message, Long userId) {
        log.warn("[安全警告] type={}, userId={}, msg={}", type, userId, message);
        String alertJson = String.format(
                "{\"type\":\"%s\",\"userId\":%d,\"message\":\"%s\",\"time\":\"%s\",\"level\":\"warning\"}",
                type, userId, message, LocalDateTime.now());
        redisTemplate.opsForList().leftPush("security:tamper_alerts", alertJson);
    }

    public boolean isUserFrozen(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("security:frozen:" + userId));
    }
}
