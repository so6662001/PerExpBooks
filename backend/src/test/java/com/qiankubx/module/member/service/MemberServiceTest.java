package com.qiankubx.module.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.coupon.service.CouponService;
import com.qiankubx.module.member.dto.CreateOrderDTO;
import com.qiankubx.module.member.dto.OrderVO;
import com.qiankubx.module.member.entity.MemberOrder;
import com.qiankubx.module.member.mapper.MemberOrderMapper;
import com.qiankubx.module.promotion.service.CommissionService;
import com.qiankubx.module.team.mapper.TeamMapper;
import com.qiankubx.module.team.mapper.TeamMemberMapper;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MemberOrderMapper memberOrderMapper;
    @Mock private UserMapper userMapper;
    @Mock private DataSignService dataSignService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CouponService couponService;
    @Mock private CommissionService commissionService;
    @Mock private TeamMapper teamMapper;
    @Mock private TeamMemberMapper teamMemberMapper;

    @InjectMocks
    private MemberService memberService;

    private User freeUser;

    @BeforeAll
    static void initCache() {
        TestLambdaCacheInitializer.initAll();
    }

    @BeforeEach
    void setUp() {
        freeUser = new User();
        freeUser.setId(1L);
        freeUser.setMemberType(0);
        freeUser.setMemberStatus(0);
        freeUser.setNickname("测试用户");
    }

    @Test
    void createOrder_monthlyPlan_shouldSucceed() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(memberOrderMapper.insert(any(MemberOrder.class))).thenAnswer(inv -> {
            MemberOrder o = inv.getArgument(0);
            o.setId(100L);
            return 1;
        });

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setPlanType(1);
        dto.setPayType(1);

        OrderVO result = memberService.createOrder(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getPlanType()).isEqualTo(1);
        assertThat(result.getPayAmount()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(result.getPayStatus()).isEqualTo(0);
        verify(memberOrderMapper).insert(any(MemberOrder.class));
    }

    @Test
    void createOrder_yearlyPlan_shouldSucceed() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(memberOrderMapper.insert(any(MemberOrder.class))).thenAnswer(inv -> {
            MemberOrder o = inv.getArgument(0);
            o.setId(101L);
            return 1;
        });

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setPlanType(2);
        dto.setPayType(1);

        OrderVO result = memberService.createOrder(1L, dto);

        assertThat(result.getPayAmount()).isEqualByComparingTo(new BigDecimal("99"));
    }

    @Test
    void createOrder_teamPlan_lessThan5_shouldThrow() {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setPlanType(3);
        dto.setPayType(1);
        dto.setTeamMemberCount(3);

        assertThatThrownBy(() -> memberService.createOrder(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("团队版最少5人起购");
    }

    @Test
    void createOrder_withCoupon_shouldNotDeductImmediately() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);
        when(couponService.calculateDiscount(eq(1L), eq(50L), any())).thenReturn(new BigDecimal("5"));
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(memberOrderMapper.insert(any(MemberOrder.class))).thenAnswer(inv -> {
            MemberOrder o = inv.getArgument(0);
            o.setId(102L);
            return 1;
        });

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setPlanType(1);
        dto.setPayType(1);
        dto.setCouponId(50L);

        OrderVO result = memberService.createOrder(1L, dto);

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(result.getPayAmount()).isEqualByComparingTo(new BigDecimal("7"));
        verify(couponService).calculateDiscount(eq(1L), eq(50L), any());
        verify(couponService, never()).useCoupon(anyLong(), anyLong(), anyLong());
    }

    @Test
    void handlePaySuccess_shouldActivateMember() {
        MemberOrder order = buildOrder(100L, 1L, 1);
        order.setPayStatus(0);
        when(memberOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(userMapper.selectById(1L)).thenReturn(freeUser);
        when(dataSignService.sign(anyString())).thenReturn("sign");

        memberService.handlePaySuccess("MB202604151234", "trade123");

        ArgumentCaptor<MemberOrder> captor = ArgumentCaptor.forClass(MemberOrder.class);
        verify(memberOrderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPayStatus()).isEqualTo(1);
        assertThat(captor.getValue().getTradeNo()).isEqualTo("trade123");
        verify(userMapper).update(isNull(), any());
    }

    @Test
    void handlePaySuccess_duplicateCall_shouldBeIdempotent() {
        MemberOrder order = buildOrder(100L, 1L, 1);
        order.setPayStatus(1);
        when(memberOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        memberService.handlePaySuccess("MB202604151234", "trade123");

        verify(memberOrderMapper, never()).updateById(any(MemberOrder.class));
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void handlePaySuccess_shouldTriggerCommission() {
        MemberOrder order = buildOrder(100L, 1L, 1);
        order.setPayStatus(0);
        when(memberOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(userMapper.selectById(1L)).thenReturn(freeUser);
        when(dataSignService.sign(anyString())).thenReturn("sign");
        doThrow(new RuntimeException("MQ unavailable"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

        memberService.handlePaySuccess("MB202604151234", "trade123");

        verify(commissionService).triggerCommission(100L);
    }

    @Test
    void applyRefund_shouldRevokeCommission() {
        MemberOrder order = buildOrder(100L, 1L, 1);
        order.setPayStatus(1);
        order.setRefundStatus(0);
        order.setPayTime(LocalDateTime.now());
        when(memberOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(dataSignService.sign(anyString())).thenReturn("sign");

        memberService.applyRefund(1L, "MB202604151234");

        verify(commissionService).revokeCommission(100L);
    }

    @Test
    void applyRefund_shouldUpdatePayStatus() {
        MemberOrder order = buildOrder(100L, 1L, 1);
        order.setPayStatus(1);
        order.setRefundStatus(0);
        order.setPayTime(LocalDateTime.now());
        when(memberOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(dataSignService.sign(anyString())).thenReturn("sign");

        memberService.applyRefund(1L, "MB202604151234");

        ArgumentCaptor<MemberOrder> captor = ArgumentCaptor.forClass(MemberOrder.class);
        verify(memberOrderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPayStatus()).isEqualTo(2);
        assertThat(captor.getValue().getRefundStatus()).isEqualTo(2);
        verify(userMapper).update(isNull(), any());
    }

    @Test
    void applyRefund_orderNotPaid_shouldThrow() {
        MemberOrder order = buildOrder(100L, 1L, 1);
        order.setPayStatus(0);
        when(memberOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> memberService.applyRefund(1L, "MB202604151234"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单未支付");
    }

    @Test
    void applyRefund_exceededRefundPeriod_shouldThrow() {
        MemberOrder order = buildOrder(100L, 1L, 1);
        order.setPayStatus(1);
        order.setRefundStatus(0);
        order.setPayTime(LocalDateTime.now().minusDays(10));
        when(memberOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> memberService.applyRefund(1L, "MB202604151234"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已超过7天退款期限");
    }

    private MemberOrder buildOrder(Long id, Long userId, int planType) {
        MemberOrder order = new MemberOrder();
        order.setId(id);
        order.setUserId(userId);
        order.setOrderNo("MB202604151234");
        order.setPlanType(planType);
        order.setOriginalAmount(new BigDecimal("12"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(new BigDecimal("12"));
        order.setPayType(1);
        order.setPayStatus(0);
        order.setIsRenewal(0);
        order.setRefundStatus(0);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }
}
