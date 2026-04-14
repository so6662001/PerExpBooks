package com.qiankubx.module.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.coupon.service.CouponService;
import com.qiankubx.module.member.dto.*;
import com.qiankubx.module.member.entity.MemberOrder;
import com.qiankubx.module.member.mapper.MemberOrderMapper;
import com.qiankubx.module.promotion.service.CommissionService;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class MemberService {

    private final MemberOrderMapper memberOrderMapper;
    private final UserMapper userMapper;
    private final DataSignService dataSignService;
    @Lazy
    private final CouponService couponService;
    @Lazy
    private final CommissionService commissionService;

    private static final BigDecimal MONTHLY_PRICE = new BigDecimal("12");
    private static final BigDecimal YEARLY_PRICE = new BigDecimal("99");
    private static final BigDecimal TEAM_PRICE = new BigDecimal("79");

    private static final int FREE_INVOICE_LIMIT = 5;
    private static final int FREE_REIMBURSE_LIMIT = 2;
    private static final int MEMBER_INVOICE_LIMIT = 999;
    private static final int MEMBER_REIMBURSE_LIMIT = 999;

    public List<PlanVO> getPlans() {
        PlanVO monthly = new PlanVO();
        monthly.setPlanType(1);
        monthly.setName("月度会员");
        monthly.setPrice(MONTHLY_PRICE);
        monthly.setOriginalPrice(MONTHLY_PRICE);
        monthly.setFeatures(List.of("无限发票录入", "无限报销单", "数据导出", "邮件发送"));
        monthly.setRecommended(false);

        PlanVO yearly = new PlanVO();
        yearly.setPlanType(2);
        yearly.setName("年度会员");
        yearly.setPrice(YEARLY_PRICE);
        yearly.setOriginalPrice(new BigDecimal("144"));
        yearly.setFeatures(List.of("无限发票录入", "无限报销单", "数据导出", "邮件发送", "优先客服", "年省45元"));
        yearly.setRecommended(true);

        PlanVO team = new PlanVO();
        team.setPlanType(3);
        team.setName("团队版");
        team.setPrice(TEAM_PRICE);
        team.setOriginalPrice(TEAM_PRICE);
        team.setFeatures(List.of("含所有个人功能", "团队协作", "成员管理", "按人数计费¥79/人/年"));
        team.setRecommended(false);

        return List.of(monthly, yearly, team);
    }

    public MemberStatusVO getMemberStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        MemberStatusVO vo = new MemberStatusVO();
        vo.setMemberType(user.getMemberType());
        vo.setMemberStatus(user.getMemberStatus());
        vo.setExpireTime(user.getMemberExpireTime());

        if (user.getMemberExpireTime() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), user.getMemberExpireTime());
            vo.setDaysLeft(Math.max(0, (int) daysLeft));
        } else {
            vo.setDaysLeft(0);
        }

        MemberStatusVO.QuotaInfo quota = new MemberStatusVO.QuotaInfo();
        quota.setMonthlyInvoiceUsed(user.getMonthlyInvoiceUsed() != null ? user.getMonthlyInvoiceUsed() : 0);
        quota.setMonthlyReimburseUsed(user.getMonthlyReimburseUsed() != null ? user.getMonthlyReimburseUsed() : 0);

        boolean isMember = user.getMemberStatus() != null && user.getMemberStatus() == 1;
        quota.setMonthlyInvoiceLimit(isMember ? MEMBER_INVOICE_LIMIT : FREE_INVOICE_LIMIT);
        quota.setMonthlyReimburseLimit(isMember ? MEMBER_REIMBURSE_LIMIT : FREE_REIMBURSE_LIMIT);
        vo.setQuotaInfo(quota);

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, CreateOrderDTO dto) {
        BigDecimal originalAmount = getPlanPrice(dto.getPlanType());
        if (dto.getPlanType() == 3 && dto.getTeamMemberCount() != null && dto.getTeamMemberCount() > 1) {
            originalAmount = TEAM_PRICE.multiply(BigDecimal.valueOf(dto.getTeamMemberCount()));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (dto.getCouponId() != null) {
            discountAmount = couponService.calculateDiscount(userId, dto.getCouponId(), originalAmount);
        }

        BigDecimal payAmount = originalAmount.subtract(discountAmount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        User user = userMapper.selectById(userId);
        boolean isRenewal = user.getMemberStatus() != null && user.getMemberStatus() == 1;

        MemberOrder order = new MemberOrder();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setPlanType(dto.getPlanType());
        order.setOriginalAmount(originalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setCouponId(dto.getCouponId());
        order.setPayType(dto.getPayType());
        order.setPayStatus(0);
        order.setIsRenewal(isRenewal ? 1 : 0);
        order.setRefundStatus(0);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        String signPayload = buildSignPayload(order);
        order.setDataSign(dataSignService.sign(signPayload));

        memberOrderMapper.insert(order);

        if (dto.getCouponId() != null) {
            couponService.useCoupon(userId, dto.getCouponId(), order.getId());
        }

        return toOrderVO(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(String orderNo, String tradeNo) {
        MemberOrder order = memberOrderMapper.selectOne(
                new LambdaQueryWrapper<MemberOrder>()
                        .eq(MemberOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            log.error("支付回调: 订单不存在, orderNo={}", orderNo);
            return;
        }
        if (order.getPayStatus() == 1) {
            log.warn("支付回调: 订单已支付, orderNo={}", orderNo);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPayStatus(1);
        order.setPayTime(now);
        order.setTradeNo(tradeNo);
        order.setUpdatedAt(now);

        User user = userMapper.selectById(order.getUserId());
        LocalDateTime memberStart;
        if (user.getMemberExpireTime() != null && user.getMemberExpireTime().isAfter(now)) {
            memberStart = user.getMemberExpireTime();
        } else {
            memberStart = now;
        }

        LocalDateTime memberEnd;
        switch (order.getPlanType()) {
            case 1 -> memberEnd = memberStart.plusMonths(1);
            case 2, 3 -> memberEnd = memberStart.plusYears(1);
            default -> throw new BizException(400, "无效的套餐类型");
        }

        order.setMemberStart(memberStart);
        order.setMemberEnd(memberEnd);

        String signPayload = buildSignPayload(order);
        order.setDataSign(dataSignService.sign(signPayload));

        memberOrderMapper.updateById(order);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, order.getUserId())
                .set(User::getMemberType, order.getPlanType())
                .set(User::getMemberStatus, 1)
                .set(User::getMemberExpireTime, memberEnd)
                .set(User::getUpdatedAt, now)
        );

        log.info("会员激活成功: userId={}, orderNo={}, planType={}, memberEnd={}",
                order.getUserId(), orderNo, order.getPlanType(), memberEnd);

        try {
            commissionService.triggerCommission(order.getId());
        } catch (Exception e) {
            log.error("触发返佣失败: orderId={}", order.getId(), e);
        }
    }

    public OrderVO getOrderDetail(Long userId, String orderNo) {
        MemberOrder order = memberOrderMapper.selectOne(
                new LambdaQueryWrapper<MemberOrder>()
                        .eq(MemberOrder::getUserId, userId)
                        .eq(MemberOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return toOrderVO(order);
    }

    public List<OrderVO> listOrders(Long userId) {
        List<MemberOrder> orders = memberOrderMapper.selectList(
                new LambdaQueryWrapper<MemberOrder>()
                        .eq(MemberOrder::getUserId, userId)
                        .orderByDesc(MemberOrder::getCreatedAt)
        );
        return orders.stream().map(this::toOrderVO).toList();
    }

    private BigDecimal getPlanPrice(Integer planType) {
        return switch (planType) {
            case 1 -> MONTHLY_PRICE;
            case 2 -> YEARLY_PRICE;
            case 3 -> TEAM_PRICE;
            default -> throw new BizException(400, "无效的套餐类型");
        };
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "MB" + timestamp + random;
    }

    private String buildSignPayload(MemberOrder order) {
        return order.getId() + "|"
                + order.getUserId() + "|"
                + order.getOrderNo() + "|"
                + order.getPlanType() + "|"
                + order.getPayAmount() + "|"
                + order.getPayStatus();
    }

    private OrderVO toOrderVO(MemberOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPlanType(order.getPlanType());
        vo.setPlanName(getPlanName(order.getPlanType()));
        vo.setOriginalAmount(order.getOriginalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayType(order.getPayType());
        vo.setPayStatus(order.getPayStatus());
        vo.setPayTime(order.getPayTime());
        vo.setTradeNo(order.getTradeNo());
        vo.setMemberStart(order.getMemberStart());
        vo.setMemberEnd(order.getMemberEnd());
        vo.setIsRenewal(order.getIsRenewal());
        vo.setRefundStatus(order.getRefundStatus());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private String getPlanName(Integer planType) {
        return switch (planType) {
            case 1 -> "月度会员";
            case 2 -> "年度会员";
            case 3 -> "团队版";
            default -> "未知";
        };
    }
}
