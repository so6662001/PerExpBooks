package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.member.entity.MemberOrder;
import com.qiankubx.module.member.mapper.MemberOrderMapper;
import com.qiankubx.module.promotion.entity.Commission;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.mapper.CommissionMapper;
import com.qiankubx.module.promotion.mapper.InvitationMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock private CommissionMapper commissionMapper;
    @Mock private InvitationMapper invitationMapper;
    @Mock private MemberOrderMapper memberOrderMapper;
    @Mock private PromoterLevelMapper promoterLevelMapper;
    @Mock private UserMapper userMapper;
    @Mock private DataSignService dataSignService;
    @Mock private PromoterLevelService promoterLevelService;

    @InjectMocks
    private CommissionService commissionService;

    @BeforeAll
    static void initCache() {
        TestLambdaCacheInitializer.initAll();
    }

    private MemberOrder paidOrder;
    private User payer;
    private PromoterLevel newbieLevel;
    private PromoterLevel goldLevel;

    @BeforeEach
    void setUp() {
        paidOrder = new MemberOrder();
        paidOrder.setId(100L);
        paidOrder.setUserId(10L);
        paidOrder.setPayStatus(1);
        paidOrder.setPayAmount(new BigDecimal("99"));
        paidOrder.setPlanType(2);

        payer = new User();
        payer.setId(10L);
        payer.setInviterId(20L);
        payer.setRootInviterId(30L);

        newbieLevel = new PromoterLevel();
        newbieLevel.setUserId(20L);
        newbieLevel.setLevel(1);
        newbieLevel.setPoints(0);

        goldLevel = new PromoterLevel();
        goldLevel.setUserId(20L);
        goldLevel.setLevel(3);
        goldLevel.setPoints(100);
    }

    @Test
    void triggerCommission_level1_shouldReturn19() {
        when(memberOrderMapper.selectById(100L)).thenReturn(paidOrder);
        when(userMapper.selectById(10L)).thenReturn(payer);
        when(promoterLevelService.getOrCreatePromoterLevel(20L)).thenReturn(newbieLevel);
        when(commissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(dataSignService.sign(anyString())).thenReturn("sign");

        PromoterLevel level2Promoter = new PromoterLevel();
        level2Promoter.setUserId(30L);
        level2Promoter.setLevel(1);
        when(promoterLevelService.getOrCreatePromoterLevel(30L)).thenReturn(level2Promoter);

        commissionService.triggerCommission(100L);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionMapper, atLeastOnce()).insert(captor.capture());
        Commission level1 = captor.getAllValues().stream()
                .filter(c -> c.getLevel() == 1).findFirst().orElse(null);
        assertThat(level1).isNotNull();
        assertThat(level1.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("19"));
        assertThat(level1.getCommissionType()).isEqualTo(1);
        verify(promoterLevelMapper).incrementFrozenBalance(20L, new BigDecimal("19"));
    }

    @Test
    void triggerCommission_goldLevel_shouldReturn22() {
        when(memberOrderMapper.selectById(100L)).thenReturn(paidOrder);
        when(userMapper.selectById(10L)).thenReturn(payer);
        when(promoterLevelService.getOrCreatePromoterLevel(20L)).thenReturn(goldLevel);
        when(commissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(dataSignService.sign(anyString())).thenReturn("sign");

        PromoterLevel level2Promoter = new PromoterLevel();
        level2Promoter.setUserId(30L);
        level2Promoter.setLevel(1);
        when(promoterLevelService.getOrCreatePromoterLevel(30L)).thenReturn(level2Promoter);

        commissionService.triggerCommission(100L);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionMapper, atLeastOnce()).insert(captor.capture());
        Commission level1 = captor.getAllValues().stream()
                .filter(c -> c.getLevel() == 1).findFirst().orElse(null);
        assertThat(level1).isNotNull();
        assertThat(level1.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("22"));
    }

    @Test
    void triggerCommission_renewal_shouldReturn10() {
        PromoterLevel silverLevel = new PromoterLevel();
        silverLevel.setUserId(20L);
        silverLevel.setLevel(2);

        when(memberOrderMapper.selectById(100L)).thenReturn(paidOrder);
        when(userMapper.selectById(10L)).thenReturn(payer);
        when(promoterLevelService.getOrCreatePromoterLevel(20L)).thenReturn(silverLevel);
        when(commissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(commissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(dataSignService.sign(anyString())).thenReturn("sign");

        PromoterLevel level2Promoter = new PromoterLevel();
        level2Promoter.setUserId(30L);
        level2Promoter.setLevel(2);
        when(promoterLevelService.getOrCreatePromoterLevel(30L)).thenReturn(level2Promoter);

        commissionService.triggerCommission(100L);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionMapper, atLeastOnce()).insert(captor.capture());
        Commission level1 = captor.getAllValues().stream()
                .filter(c -> c.getLevel() == 1).findFirst().orElse(null);
        assertThat(level1).isNotNull();
        assertThat(level1.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(level1.getCommissionType()).isEqualTo(2);
    }

    @Test
    void triggerCommission_renewal_newbieLevel_shouldSkip() {
        when(memberOrderMapper.selectById(100L)).thenReturn(paidOrder);
        when(userMapper.selectById(10L)).thenReturn(payer);
        when(promoterLevelService.getOrCreatePromoterLevel(20L)).thenReturn(newbieLevel);
        when(commissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        PromoterLevel level2Promoter = new PromoterLevel();
        level2Promoter.setUserId(30L);
        level2Promoter.setLevel(1);
        lenient().when(promoterLevelService.getOrCreatePromoterLevel(30L)).thenReturn(level2Promoter);
        lenient().when(commissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        commissionService.triggerCommission(100L);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionMapper, never()).insert(captor.capture());
    }

    @Test
    void triggerCommission_monthlyCapExceeded_shouldSkip() {
        Commission existing = new Commission();
        existing.setCommissionAmount(new BigDecimal("4990"));
        existing.setStatus(0);

        when(memberOrderMapper.selectById(100L)).thenReturn(paidOrder);
        when(userMapper.selectById(10L)).thenReturn(payer);
        when(promoterLevelService.getOrCreatePromoterLevel(20L)).thenReturn(newbieLevel);
        when(commissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));

        PromoterLevel level2Promoter = new PromoterLevel();
        level2Promoter.setUserId(30L);
        level2Promoter.setLevel(1);
        when(promoterLevelService.getOrCreatePromoterLevel(30L)).thenReturn(level2Promoter);

        commissionService.triggerCommission(100L);

        verify(commissionMapper, never()).insert(any(Commission.class));
    }

    @Test
    void triggerCommission_level2_shouldReturn5() {
        when(memberOrderMapper.selectById(100L)).thenReturn(paidOrder);
        when(userMapper.selectById(10L)).thenReturn(payer);
        when(promoterLevelService.getOrCreatePromoterLevel(20L)).thenReturn(newbieLevel);
        when(commissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(dataSignService.sign(anyString())).thenReturn("sign");

        PromoterLevel level2Promoter = new PromoterLevel();
        level2Promoter.setUserId(30L);
        level2Promoter.setLevel(1);
        when(promoterLevelService.getOrCreatePromoterLevel(30L)).thenReturn(level2Promoter);

        commissionService.triggerCommission(100L);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionMapper, atLeastOnce()).insert(captor.capture());
        Commission level2 = captor.getAllValues().stream()
                .filter(c -> c.getLevel() == 2).findFirst().orElse(null);
        assertThat(level2).isNotNull();
        assertThat(level2.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    void settleCommission_alreadySettled_shouldSkip() {
        Commission commission = new Commission();
        commission.setId(1L);
        commission.setStatus(1);
        when(commissionMapper.selectById(1L)).thenReturn(commission);

        commissionService.settleCommission(1L);

        verify(commissionMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void revokeCommission_shouldRevertBalances() {
        Commission frozenCommission = new Commission();
        frozenCommission.setId(1L);
        frozenCommission.setUserId(20L);
        frozenCommission.setOrderId(100L);
        frozenCommission.setCommissionAmount(new BigDecimal("19"));
        frozenCommission.setStatus(0);

        Commission settledCommission = new Commission();
        settledCommission.setId(2L);
        settledCommission.setUserId(30L);
        settledCommission.setOrderId(100L);
        settledCommission.setCommissionAmount(new BigDecimal("5"));
        settledCommission.setStatus(1);

        when(commissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(frozenCommission, settledCommission));

        commissionService.revokeCommission(100L);

        verify(promoterLevelMapper).decrementFrozenBalance(20L, new BigDecimal("19"));
        verify(promoterLevelMapper).decrementAvailableBalance(30L, new BigDecimal("5"));
        verify(commissionMapper, times(2)).updateById(any(Commission.class));
    }
}
