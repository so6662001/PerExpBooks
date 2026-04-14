package com.qiankubx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
                    .set(User::getUpdatedAt, LocalDateTime.now())
            );
            log.info("[定时任务] 会员已过期: userId={}, expireTime={}", user.getId(), user.getMemberExpireTime());
        }
        log.info("[定时任务] 会员到期检查完成, 过期数量: {}", expiredUsers.size());
    }
}
