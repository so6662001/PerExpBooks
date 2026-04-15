package com.qiankubx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.module.coupon.service.CouponService;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberExpireTask {

    private final UserMapper userMapper;
    private final CouponService couponService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void checkMemberExpire() {
        log.info("[定时任务] 检查会员到期...");
        List<User> expiredUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getMemberStatus, 1)
                        .lt(User::getMemberExpireTime, LocalDateTime.now())
        );

        for (User user : expiredUsers) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getMemberStatus, 0)
                    .set(User::getMemberType, 0)
                    .set(User::getUpdatedAt, LocalDateTime.now())
            );
            log.info("[定时任务] 会员已过期: userId={}, expireTime={}", user.getId(), user.getMemberExpireTime());
        }
        log.info("[定时任务] 会员到期检查完成, 过期数量: {}", expiredUsers.size());

        List<User> recallUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getMemberStatus, 0)
                        .between(User::getMemberExpireTime,
                                LocalDateTime.now().minusDays(8),
                                LocalDateTime.now().minusDays(7))
        );
        for (User u : recallUsers) {
            try {
                couponService.issueRecallCoupon(u.getId());
                log.info("[定时任务] 发放唤回券: userId={}", u.getId());
            } catch (Exception e) {
                log.error("[定时任务] 发放唤回券失败: userId={}", u.getId(), e);
            }
        }
        log.info("[定时任务] 唤回券发放完成, 数量: {}", recallUsers.size());
    }
}
