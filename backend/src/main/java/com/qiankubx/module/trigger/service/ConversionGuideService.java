package com.qiankubx.module.trigger.service;

import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionGuideService {

    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> checkConversion(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("triggered", false);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return result;
        }

        boolean isMember = user.getMemberStatus() != null && user.getMemberStatus() == 1;
        int invoiceUsed = user.getMonthlyInvoiceUsed() != null ? user.getMonthlyInvoiceUsed() : 0;

        if (!isMember) {
            if (invoiceUsed == 4) {
                result.put("triggered", true);
                result.put("type", "quota_warning");
                result.put("title", "额度提醒");
                result.put("message", "本月已使用4/5张发票额度，升级会员享无限额度");
                result.put("buttonText", "查看会员");
                return result;
            }
            if (invoiceUsed >= 5) {
                result.put("triggered", true);
                result.put("type", "quota_exceeded");
                result.put("title", "额度已满");
                result.put("message", "本月免费额度已用完，升级会员立即继续使用");
                result.put("buttonText", "立即升级");
                return result;
            }
        }

        if (isMember) {
            Long renewalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_member_order WHERE user_id = ? AND pay_status = 1 AND plan_type = 1",
                    Long.class, userId);
            if (renewalCount != null && renewalCount >= 3) {
                Integer currentPlan = user.getMemberType();
                if (currentPlan != null && currentPlan == 1) {
                    result.put("triggered", true);
                    result.put("type", "recommend_yearly");
                    result.put("title", "推荐年度会员");
                    result.put("message", "您已连续续费" + renewalCount + "个月，年度会员更划算，可省45元");
                    result.put("buttonText", "了解年度会员");
                    return result;
                }
            }

            if (user.getMemberExpireTime() != null) {
                long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), user.getMemberExpireTime());
                if (daysLeft >= 0 && daysLeft <= 3) {
                    result.put("triggered", true);
                    result.put("type", "renewal_reminder");
                    result.put("title", "会员即将到期");
                    result.put("message", "您的会员将在" + daysLeft + "天后到期，续费保持全部功能");
                    result.put("buttonText", "立即续费");
                    return result;
                }
            }
        }

        Long sameCompanyInvites = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_invitation i
                JOIN t_user u ON i.invitee_id = u.id
                WHERE i.inviter_id = ? AND i.level = 1
                AND u.company IS NOT NULL AND u.company = (SELECT company FROM t_user WHERE id = ?)
                """, Long.class, userId, userId);

        if (sameCompanyInvites != null && sameCompanyInvites >= 3) {
            result.put("triggered", true);
            result.put("type", "recommend_team");
            result.put("title", "推荐团队版");
            result.put("message", "您已邀请" + sameCompanyInvites + "位同公司同事，团队版更高效");
            result.put("buttonText", "了解团队版");
            return result;
        }

        return result;
    }
}
