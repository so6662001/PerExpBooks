package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.ChainHashService;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.coupon.service.CouponService;
import com.qiankubx.module.promotion.entity.PointsLog;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.mapper.PointsLogMapper;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import com.qiankubx.TestLambdaCacheInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    @Mock private PointsLogMapper pointsLogMapper;
    @Mock private PromoterLevelMapper promoterLevelMapper;
    @Mock private PromoterLevelService promoterLevelService;
    @Mock private ChainHashService chainHashService;
    @Mock private DataSignService dataSignService;
    @Mock private UserMapper userMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private CouponService couponService;

    @InjectMocks
    private PointsService pointsService;

    private PromoterLevel promoterLevel;

    @BeforeAll
    static void initCache() {
        TestLambdaCacheInitializer.initAll();
    }

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        promoterLevel = new PromoterLevel();
        promoterLevel.setUserId(1L);
        promoterLevel.setLevel(1);
        promoterLevel.setPoints(100);
        promoterLevel.setTotalPoints(200);
        promoterLevel.setAvailableBalance(BigDecimal.ZERO);
        promoterLevel.setFrozenBalance(BigDecimal.ZERO);
        promoterLevel.setWithdrawnAmount(BigDecimal.ZERO);
        promoterLevel.setTotalCommission(BigDecimal.ZERO);
        promoterLevel.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void addPoints_shouldIncrementBalance() {
        when(valueOps.setIfAbsent(eq("lock:points:1"), eq("1"), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);
        when(pointsLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(chainHashService.computeHash(anyString())).thenReturn("chain-hash-abc");
        when(dataSignService.sign(anyString())).thenReturn("data-sign-abc");
        when(pointsLogMapper.insert(any(PointsLog.class))).thenAnswer(inv -> {
            PointsLog log = inv.getArgument(0);
            log.setId(1L);
            return 1;
        });

        pointsService.addPoints(1L, 5, "invite_register", 2L, "邀请好友注册");

        ArgumentCaptor<PointsLog> insertCaptor = ArgumentCaptor.forClass(PointsLog.class);
        verify(pointsLogMapper).insert(insertCaptor.capture());
        PointsLog inserted = insertCaptor.getValue();
        assertThat(inserted.getPoints()).isEqualTo(5);
        assertThat(inserted.getBalanceAfter()).isEqualTo(105);
        assertThat(inserted.getAction()).isEqualTo("invite_register");

        verify(promoterLevelMapper).incrementPoints(1L, 5, 5);
        verify(stringRedisTemplate).delete("lock:points:1");
    }

    @Test
    void addPoints_shouldRecordFlowLog() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);
        when(pointsLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(chainHashService.computeHash(anyString())).thenReturn("hash");
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(pointsLogMapper.insert(any(PointsLog.class))).thenAnswer(inv -> {
            PointsLog log = inv.getArgument(0);
            log.setId(2L);
            return 1;
        });

        pointsService.addPoints(1L, 10, "daily_share", null, "每日分享");

        ArgumentCaptor<PointsLog> captor = ArgumentCaptor.forClass(PointsLog.class);
        verify(pointsLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getRemark()).isEqualTo("每日分享");
        assertThat(captor.getValue().getAction()).isEqualTo("daily_share");

        verify(pointsLogMapper).updateById(any(PointsLog.class));
    }

    @Test
    void addPoints_shouldSetChainHash() {
        PointsLog prevLog = new PointsLog();
        prevLog.setId(5L);
        prevLog.setChainHash("prev-chain-hash");

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);
        when(pointsLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(prevLog);
        when(chainHashService.computeHash(anyString())).thenReturn("new-chain-hash");
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(pointsLogMapper.insert(any(PointsLog.class))).thenAnswer(inv -> {
            PointsLog log = inv.getArgument(0);
            log.setId(6L);
            return 1;
        });

        pointsService.addPoints(1L, 5, "invite_register", 3L, "邀请注册");

        ArgumentCaptor<PointsLog> insertCaptor = ArgumentCaptor.forClass(PointsLog.class);
        verify(pointsLogMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getPrevHash()).isEqualTo("prev-chain-hash");

        ArgumentCaptor<PointsLog> updateCaptor = ArgumentCaptor.forClass(PointsLog.class);
        verify(pointsLogMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getChainHash()).isEqualTo("new-chain-hash");
    }

    @Test
    void redeemPoints_7days_shouldExtendMember() {
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);
        when(pointsLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(chainHashService.computeHash(anyString())).thenReturn("hash");
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(promoterLevelService.buildPromoterSignPayload(any())).thenReturn("payload");
        when(pointsLogMapper.insert(any(PointsLog.class))).thenAnswer(inv -> {
            PointsLog log = inv.getArgument(0);
            log.setId(10L);
            return 1;
        });

        User user = new User();
        user.setId(1L);
        user.setMemberStatus(0);
        user.setMemberExpireTime(null);
        when(userMapper.selectById(1L)).thenReturn(user);

        pointsService.redeemPoints(1L, 1);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getMemberStatus()).isEqualTo(1);
        assertThat(captor.getValue().getMemberExpireTime()).isNotNull();
    }

    @Test
    void redeemPoints_insufficientPoints_shouldThrow() {
        promoterLevel.setPoints(30);
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);

        assertThatThrownBy(() -> pointsService.redeemPoints(1L, 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("积分不足");
    }

    @Test
    void addPoints_concurrentLockFail_shouldThrow() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> pointsService.addPoints(1L, 5, "test", null, "test"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("操作过于频繁");
    }
}
