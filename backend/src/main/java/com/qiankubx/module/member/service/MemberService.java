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
import com.qiankubx.common.config.RabbitMQConfig;
import com.qiankubx.module.promotion.service.CommissionService;
import com.qiankubx.module.team.entity.Team;
import com.qiankubx.module.team.entity.TeamMember;
import com.qiankubx.module.team.mapper.TeamMapper;
import com.qiankubx.module.team.mapper.TeamMemberMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class MemberService {

    private final MemberOrderMapper memberOrderMapper;
    private final UserMapper userMapper;
    private final DataSignService dataSignService;
    private final RabbitTemplate rabbitTemplate;
    @Lazy
    private final CouponService couponService;
    @Lazy
    private final CommissionService commissionService;
    @Lazy
    private final TeamMapper teamMapper;
    @Lazy
    private final TeamMemberMapper teamMemberMapper;

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
        if (dto.getPlanType() == 3) {
            if (dto.getTeamMemberCount() == null || dto.getTeamMemberCount() < 5) {
                throw new BizException(400, "团队版最少5人起购");
            }
        }

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
        order.setTeamMemberCount(dto.getTeamMemberCount());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        String signPayload = buildSignPayload(order);
        order.setDataSign(dataSignService.sign(signPayload));

        memberOrderMapper.insert(order);

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

        if (order.getCouponId() != null) {
            try {
                couponService.useCoupon(order.getUserId(), order.getCouponId(), order.getId());
            } catch (Exception e) {
                log.error("支付成功后扣减优惠券失败: orderId={}, couponId={}", order.getId(), order.getCouponId(), e);
            }
        }

        if (order.getPlanType() == 3) {
            Team team = teamMapper.selectOne(new LambdaQueryWrapper<Team>()
                    .eq(Team::getOwnerId, user.getId())
                    .eq(Team::getStatus, 0));
            if (team == null) {
                team = new Team();
                team.setName(user.getNickname() + "的团队");
                team.setOwnerId(user.getId());
                team.setInviteCode(generateTeamInviteCode());
                team.setMaxMember(order.getTeamMemberCount() != null ? order.getTeamMemberCount() : 5);
                team.setMemberCount(1);
                team.setStatus(0);
                team.setCreatedAt(now);
                team.setUpdatedAt(now);
                teamMapper.insert(team);

                TeamMember member = new TeamMember();
                member.setTeamId(team.getId());
                member.setUserId(user.getId());
                member.setRole(1);
                member.setJoinedAt(now);
                member.setStatus(0);
                member.setCreatedAt(now);
                teamMemberMapper.insert(member);
            }
            user.setTeamId(team.getId());
            order.setTeamId(team.getId());
        }

        String signPayload = buildSignPayload(order);
        order.setDataSign(dataSignService.sign(signPayload));

        memberOrderMapper.updateById(order);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, order.getUserId())
                .set(User::getMemberType, order.getPlanType())
                .set(User::getMemberStatus, 1)
                .set(User::getMemberExpireTime, memberEnd)
                .set(order.getPlanType() == 3, User::getTeamId, order.getTeamId())
                .set(User::getUpdatedAt, now)
        );

        log.info("会员激活成功: userId={}, orderNo={}, planType={}, memberEnd={}",
                order.getUserId(), orderNo, order.getPlanType(), memberEnd);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_QIANKU,
                    RabbitMQConfig.ROUTING_PAY_SUCCESS,
                    Map.of("orderId", order.getId(), "userId", order.getUserId()));
            log.info("支付成功MQ消息已发送: orderId={}, userId={}", order.getId(), order.getUserId());
        } catch (Exception e) {
            log.error("发送支付成功MQ消息失败，降级为同步处理: orderId={}", order.getId(), e);
            try {
                commissionService.triggerCommission(order.getId());
            } catch (Exception ex) {
                log.error("同步触发返佣也失败: orderId={}", order.getId(), ex);
            }
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

    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long userId, String orderNo) {
        MemberOrder order = memberOrderMapper.selectOne(
                new LambdaQueryWrapper<MemberOrder>()
                        .eq(MemberOrder::getUserId, userId)
                        .eq(MemberOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (order.getPayStatus() != 1) {
            throw new BizException(400, "订单未支付，无法退款");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() != 0) {
            throw new BizException(400, "退款已处理");
        }
        if (order.getPayTime() != null && order.getPayTime().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BizException(400, "已超过7天退款期限");
        }

        order.setPayStatus(2);
        order.setRefundStatus(2);
        order.setRefundTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        String signPayload = buildSignPayload(order);
        order.setDataSign(dataSignService.sign(signPayload));
        memberOrderMapper.updateById(order);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getMemberType, 0)
                .set(User::getMemberStatus, 0)
                .set(User::getMemberExpireTime, null)
                .set(User::getUpdatedAt, LocalDateTime.now())
        );

        try {
            commissionService.revokeCommission(order.getId());
        } catch (Exception e) {
            log.error("退款撤销返佣失败: orderId={}", order.getId(), e);
        }

        log.info("退款成功: userId={}, orderNo={}", userId, orderNo);
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

    private String generateTeamInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String buildSignPayload(MemberOrder order) {
        return order.getId() + "|" + order.getUserId() + "|" + order.getPayAmount() + "|"
                + order.getPlanType() + "|" + order.getPayStatus() + "|" + order.getPayTime()
                + "|" + order.getCreatedAt();
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
