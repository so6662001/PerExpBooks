package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.common.security.TamperAlertService;
import com.qiankubx.common.util.AesUtil;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.entity.Withdrawal;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import com.qiankubx.module.promotion.mapper.WithdrawalMapper;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock private WithdrawalMapper withdrawalMapper;
    @Mock private PromoterLevelMapper promoterLevelMapper;
    @Mock private PromoterLevelService promoterLevelService;
    @Mock private DataSignService dataSignService;
    @Mock private AntiCheatService antiCheatService;
    @Mock private TamperAlertService tamperAlertService;
    @Mock private AesUtil aesUtil;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private WithdrawalService withdrawalService;

    private PromoterLevel promoterLevel;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        promoterLevel = new PromoterLevel();
        promoterLevel.setUserId(1L);
        promoterLevel.setAvailableBalance(new BigDecimal("500"));
        promoterLevel.setFrozenBalance(BigDecimal.ZERO);
    }

    @Test
    void applyWithdraw_belowMinimum_shouldThrow() {
        assertThatThrownBy(() -> withdrawalService.applyWithdraw(1L, new BigDecimal("30"), 1, "account"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("最低提现金额为50元");
    }

    @Test
    void applyWithdraw_insufficientBalance_shouldThrow() {
        promoterLevel.setAvailableBalance(new BigDecimal("40"));
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);

        assertThatThrownBy(() -> withdrawalService.applyWithdraw(1L, new BigDecimal("50"), 1, "account"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("可提现余额不足");
    }

    @Test
    void applyWithdraw_frozenUser_shouldThrow() {
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);
        when(tamperAlertService.isUserFrozen(1L)).thenReturn(true);

        assertThatThrownBy(() -> withdrawalService.applyWithdraw(1L, new BigDecimal("100"), 1, "account"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("账户资金操作已被暂时冻结");
    }

    @Test
    void applyWithdraw_exceededMonthlyLimit_shouldThrow() {
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);
        when(tamperAlertService.isUserFrozen(1L)).thenReturn(false);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(withdrawalMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        assertThatThrownBy(() -> withdrawalService.applyWithdraw(1L, new BigDecimal("100"), 1, "account"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("每月最多提现2次");

        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    void applyWithdraw_valid_shouldDeductBalance() {
        when(promoterLevelService.getOrCreatePromoterLevel(1L)).thenReturn(promoterLevel);
        when(tamperAlertService.isUserFrozen(1L)).thenReturn(false);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(withdrawalMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(promoterLevelMapper.deductAvailableBalance(1L, new BigDecimal("100"))).thenReturn(1);
        when(aesUtil.encrypt("account_info")).thenReturn("encrypted");
        when(dataSignService.sign(anyString())).thenReturn("sign");

        withdrawalService.applyWithdraw(1L, new BigDecimal("100"), 1, "account_info");

        verify(promoterLevelMapper).deductAvailableBalance(1L, new BigDecimal("100"));

        ArgumentCaptor<Withdrawal> captor = ArgumentCaptor.forClass(Withdrawal.class);
        verify(withdrawalMapper).insert(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        verify(stringRedisTemplate).delete(anyString());
    }
}
