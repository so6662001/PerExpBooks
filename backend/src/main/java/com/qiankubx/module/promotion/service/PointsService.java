package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.ChainHashService;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.coupon.service.CouponService;
import com.qiankubx.module.promotion.dto.PointsLogVO;
import com.qiankubx.module.promotion.entity.PointsLog;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.mapper.PointsLogMapper;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class PointsService {

    private final PointsLogMapper pointsLogMapper;
    private final PromoterLevelMapper promoterLevelMapper;
    private final PromoterLevelService promoterLevelService;
    private final ChainHashService chainHashService;
    private final DataSignService dataSignService;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    @Lazy
    private final CouponService couponService;

    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, int points, String action, Long refId, String remark) {
        PromoterLevel level = promoterLevelService.getOrCreatePromoterLevel(userId);

        PointsLog lastLog = pointsLogMapper.selectOne(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .orderByDesc(PointsLog::getId)
                        .last("LIMIT 1")
        );
        String previousHash = lastLog != null ? lastLog.getChainHash() : null;

        int balanceAfter = level.getPoints() + points;

        PointsLog logEntry = new PointsLog();
        logEntry.setUserId(userId);
        logEntry.setPoints(points);
        logEntry.setBalanceAfter(balanceAfter);
        logEntry.setAction(action);
        logEntry.setRefId(refId);
        logEntry.setRemark(remark);
        logEntry.setCreatedAt(LocalDateTime.now());

        String payload = userId + "|" + points + "|" + balanceAfter + "|" + action + "|" + logEntry.getCreatedAt();
        logEntry.setChainHash(chainHashService.computeChainHash(previousHash, payload));
        logEntry.setDataSign(dataSignService.sign(payload));

        pointsLogMapper.insert(logEntry);

        promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                .eq(PromoterLevel::getUserId, userId)
                .setSql("points = points + " + points)
                .setSql("total_points = total_points + " + points)
                .set(PromoterLevel::getUpdatedAt, LocalDateTime.now())
        );

        log.info("积分变动: userId={}, points={}, action={}, balanceAfter={}", userId, points, action, balanceAfter);
    }

    @Transactional(rollbackFor = Exception.class)
    public void redeemPoints(Long userId, Integer redeemType) {
        int requiredPoints;
        String remark;

        switch (redeemType) {
            case 1 -> { requiredPoints = 50; remark = "兑换7天会员"; }
            case 2 -> { requiredPoints = 100; remark = "兑换¥10优惠券"; }
            case 3 -> { requiredPoints = 200; remark = "兑换1月会员"; }
            case 4 -> { requiredPoints = 500; remark = "兑换年度会员"; }
            default -> throw new BizException(400, "无效的兑换类型");
        }

        PromoterLevel level = promoterLevelService.getOrCreatePromoterLevel(userId);
        if (level.getPoints() < requiredPoints) {
            throw new BizException(400, "积分不足，需要" + requiredPoints + "积分，当前" + level.getPoints() + "积分");
        }

        PointsLog lastLog = pointsLogMapper.selectOne(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .orderByDesc(PointsLog::getId)
                        .last("LIMIT 1")
        );
        String previousHash = lastLog != null ? lastLog.getChainHash() : null;

        int balanceAfter = level.getPoints() - requiredPoints;

        PointsLog logEntry = new PointsLog();
        logEntry.setUserId(userId);
        logEntry.setPoints(-requiredPoints);
        logEntry.setBalanceAfter(balanceAfter);
        logEntry.setAction("redeem");
        logEntry.setRemark(remark);
        logEntry.setCreatedAt(LocalDateTime.now());

        String payload = userId + "|" + (-requiredPoints) + "|" + balanceAfter + "|redeem|" + logEntry.getCreatedAt();
        logEntry.setChainHash(chainHashService.computeChainHash(previousHash, payload));
        logEntry.setDataSign(dataSignService.sign(payload));

        pointsLogMapper.insert(logEntry);

        promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                .eq(PromoterLevel::getUserId, userId)
                .set(PromoterLevel::getPoints, balanceAfter)
                .set(PromoterLevel::getUpdatedAt, LocalDateTime.now())
        );

        log.info("积分兑换: userId={}, type={}, points={}, remark={}", userId, redeemType, -requiredPoints, remark);

        User user = userMapper.selectById(userId);
        switch (redeemType) {
            case 1 -> {
                if (user.getMemberExpireTime() != null && user.getMemberExpireTime().isAfter(LocalDateTime.now())) {
                    user.setMemberExpireTime(user.getMemberExpireTime().plusDays(7));
                } else {
                    user.setMemberType(2);
                    user.setMemberStatus(1);
                    user.setMemberExpireTime(LocalDateTime.now().plusDays(7));
                }
                userMapper.updateById(user);
            }
            case 2 -> {
                couponService.issueRedeemCoupon(userId, new BigDecimal("10"));
            }
            case 3 -> {
                if (user.getMemberExpireTime() != null && user.getMemberExpireTime().isAfter(LocalDateTime.now())) {
                    user.setMemberExpireTime(user.getMemberExpireTime().plusMonths(1));
                } else {
                    user.setMemberType(1);
                    user.setMemberStatus(1);
                    user.setMemberExpireTime(LocalDateTime.now().plusMonths(1));
                }
                userMapper.updateById(user);
            }
            case 4 -> {
                if (user.getMemberExpireTime() != null && user.getMemberExpireTime().isAfter(LocalDateTime.now())) {
                    user.setMemberExpireTime(user.getMemberExpireTime().plusYears(1));
                } else {
                    user.setMemberType(2);
                    user.setMemberStatus(1);
                    user.setMemberExpireTime(LocalDateTime.now().plusYears(1));
                }
                userMapper.updateById(user);
            }
        }
    }

    public void recordDailyShare(Long userId) {
        String key = "daily_share:" + userId + ":" + LocalDate.now();
        Boolean alreadyShared = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(alreadyShared)) {
            return;
        }

        addPoints(userId, 2, "daily_share", null, "每日分享");
        stringRedisTemplate.opsForValue().set(key, "1", 2, TimeUnit.DAYS);

        int consecutiveDays = 0;
        for (int i = 0; i < 7; i++) {
            String dayKey = "daily_share:" + userId + ":" + LocalDate.now().minusDays(i);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(dayKey))) {
                consecutiveDays++;
            } else {
                break;
            }
        }
        if (consecutiveDays == 7) {
            addPoints(userId, 20, "share_streak_7", null, "连续7天分享奖励");
        }
    }

    public List<PointsLogVO> getPointsLog(Long userId) {
        List<PointsLog> logs = pointsLogMapper.selectList(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .orderByDesc(PointsLog::getCreatedAt)
        );
        return logs.stream().map(this::toVO).toList();
    }

    private PointsLogVO toVO(PointsLog log) {
        PointsLogVO vo = new PointsLogVO();
        vo.setId(log.getId());
        vo.setPoints(log.getPoints());
        vo.setBalanceAfter(log.getBalanceAfter());
        vo.setAction(log.getAction());
        vo.setRemark(log.getRemark());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
