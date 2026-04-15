package com.qiankubx.module.promotion.service;

import com.qiankubx.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntiCheatService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private static final int MAX_BIND_PER_CODE_PER_DAY = 10;
    private static final String KEY_PREFIX_IP = "anticheat:invite:ip:";
    private static final String KEY_PREFIX_DEVICE = "anticheat:invite:device:";
    private static final String KEY_PREFIX_CODE = "anticheat:invite:code:";

    public Map<String, Object> checkInviteCheat(Long inviterId, String inviteeIp, String deviceFingerprint) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("blocked", false);

        if (inviteeIp != null && !inviteeIp.isBlank()) {
            String ipKey = KEY_PREFIX_IP + inviteeIp;
            String ipCount = stringRedisTemplate.opsForValue().get(ipKey);
            if (ipCount != null) {
                result.put("blocked", true);
                result.put("reason", "同IP 24小时内已注册");
                log.warn("邀请作弊检测-同IP: inviterId={}, ip={}", inviterId, inviteeIp);
                return result;
            }
            stringRedisTemplate.opsForValue().set(ipKey, "1", 24, TimeUnit.HOURS);
        }

        if (deviceFingerprint != null && !deviceFingerprint.isBlank()) {
            String deviceKey = KEY_PREFIX_DEVICE + deviceFingerprint;
            String deviceCount = stringRedisTemplate.opsForValue().get(deviceKey);
            if (deviceCount != null) {
                result.put("blocked", true);
                result.put("reason", "同设备 24小时内已注册");
                log.warn("邀请作弊检测-同设备: inviterId={}, device={}", inviterId, deviceFingerprint);
                return result;
            }
            stringRedisTemplate.opsForValue().set(deviceKey, "1", 24, TimeUnit.HOURS);
        }

        String codeKey = KEY_PREFIX_CODE + inviterId;
        Long codeCount = stringRedisTemplate.opsForValue().increment(codeKey);
        if (codeCount != null && codeCount == 1) {
            stringRedisTemplate.expire(codeKey, 24, TimeUnit.HOURS);
        }
        if (codeCount != null && codeCount > MAX_BIND_PER_CODE_PER_DAY) {
            result.put("blocked", true);
            result.put("reason", "同邀请码24小时内绑定超过" + MAX_BIND_PER_CODE_PER_DAY + "人");
            log.warn("邀请作弊检测-超限: inviterId={}, count={}", inviterId, codeCount);
            return result;
        }

        return result;
    }

    public void assertInviteNotBlocked(Long inviterId, String inviteeIp, String deviceFingerprint) {
        Map<String, Object> result = checkInviteCheat(inviterId, inviteeIp, deviceFingerprint);
        if (Boolean.TRUE.equals(result.get("blocked"))) {
            throw new BizException(400, "邀请风控拦截: " + result.get("reason"));
        }
    }

    public void assertWithdrawalNotFlagged(Long userId) {
        Map<String, Object> result = checkWithdrawalCheat(userId);
        if (Boolean.TRUE.equals(result.get("flagged"))) {
            throw new BizException(400, "提现风控拦截: " + result.get("reason"));
        }
    }

    public Map<String, Object> checkWithdrawalCheat(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flagged", false);
        result.put("userId", userId);

        try {
            Long totalInvites = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_invitation WHERE inviter_id = ? AND level = 1",
                    Long.class, userId);
            Long paidInvites = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_invitation WHERE inviter_id = ? AND level = 1 AND invitee_status = 1",
                    Long.class, userId);

            if (totalInvites != null && totalInvites > 0 && paidInvites != null) {
                double conversionRate = (double) paidInvites / totalInvites;
                if (conversionRate > 0.5 && totalInvites >= 5) {
                    result.put("flagged", true);
                    result.put("reason", "邀请转化率异常: " + String.format("%.1f%%", conversionRate * 100));
                    result.put("totalInvites", totalInvites);
                    result.put("paidInvites", paidInvites);
                    log.warn("提现风控-转化率异常: userId={}, rate={}", userId, conversionRate);
                    return result;
                }
            }

            result.put("message", "提现风控检查通过");
        } catch (Exception e) {
            log.error("提现风控检查异常: userId={}", userId, e);
            result.put("message", "风控检查异常");
        }

        return result;
    }
}
