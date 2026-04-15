package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.member.entity.MemberOrder;
import com.qiankubx.module.member.mapper.MemberOrderMapper;
import com.qiankubx.module.promotion.dto.CommissionVO;
import com.qiankubx.module.promotion.entity.Commission;
import com.qiankubx.module.promotion.entity.Invitation;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.mapper.CommissionMapper;
import com.qiankubx.module.promotion.mapper.InvitationMapper;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionMapper commissionMapper;
    private final InvitationMapper invitationMapper;
    private final MemberOrderMapper memberOrderMapper;
    private final PromoterLevelMapper promoterLevelMapper;
    private final UserMapper userMapper;
    private final DataSignService dataSignService;
    private final PromoterLevelService promoterLevelService;

    @Transactional(rollbackFor = Exception.class)
    public void triggerCommission(Long orderId) {
        MemberOrder order = memberOrderMapper.selectById(orderId);
        if (order == null || order.getPayStatus() != 1) {
            return;
        }

        Long payerId = order.getUserId();
        User payer = userMapper.selectById(payerId);
        if (payer == null || payer.getInviterId() == null) {
            log.info("无邀请关系，不触发返佣: userId={}", payerId);
            return;
        }

        Long level1InviterId = payer.getInviterId();
        PromoterLevel level1Promoter = promoterLevelService.getOrCreatePromoterLevel(level1InviterId);

        BigDecimal level1Amount;
        BigDecimal level2Amount;
        switch (level1Promoter.getLevel()) {
            case 3:  level1Amount = new BigDecimal("22"); level2Amount = new BigDecimal("8"); break;
            case 4:  level1Amount = new BigDecimal("25"); level2Amount = new BigDecimal("10"); break;
            default: level1Amount = new BigDecimal("19"); level2Amount = new BigDecimal("5"); break;
        }

        boolean isRenewal = commissionMapper.selectCount(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getUserId, level1InviterId)
                        .eq(Commission::getInviteeId, payerId)
                        .in(Commission::getCommissionType, 1, 2)
        ) > 0;

        int commissionType;
        if (isRenewal) {
            if (level1Promoter.getLevel() >= 2) {
                level1Amount = new BigDecimal("10");
                commissionType = 2;
            } else {
                log.info("新手不享受续费返佣: inviterId={}", level1InviterId);
                handleLevel2Commission(payer, orderId, level2Amount, true, level1Promoter);
                updateInvitationStatus(level1InviterId, payerId);
                return;
            }
        } else {
            commissionType = 1;
        }

        if (checkMonthlyLimit(level1InviterId, level1Amount, 1)) {
            Commission level1Commission = new Commission();
            level1Commission.setUserId(level1InviterId);
            level1Commission.setOrderId(orderId);
            level1Commission.setInviteeId(payerId);
            level1Commission.setLevel(1);
            level1Commission.setCommissionType(commissionType);
            level1Commission.setOrderAmount(order.getPayAmount());
            level1Commission.setCommissionRate(level1Amount);
            level1Commission.setCommissionAmount(level1Amount);
            level1Commission.setStatus(0);
            level1Commission.setCreatedAt(LocalDateTime.now());

            String signPayload = buildCommissionSignPayload(level1Commission);
            level1Commission.setDataSign(dataSignService.sign(signPayload));

            commissionMapper.insert(level1Commission);
            addFrozenBalance(level1InviterId, level1Amount);

            log.info("一级返佣: inviterId={}, amount={}, type={}", level1InviterId, level1Amount, commissionType);
        } else {
            log.info("一级返佣超月封顶: inviterId={}, amount={}", level1InviterId, level1Amount);
        }

        if (!isRenewal) {
            updateInvitationStatus(level1InviterId, payerId);
            promoterLevelService.incrementPaidInviteCount(level1InviterId);
            promoterLevelService.checkAndUpgrade(level1InviterId);
        }

        handleLevel2Commission(payer, orderId, level2Amount, isRenewal, level1Promoter);
    }

    private void handleLevel2Commission(User payer, Long orderId, BigDecimal level2BaseAmount,
                                         boolean isRenewal, PromoterLevel level1Promoter) {
        Long level2InviterId = payer.getRootInviterId();
        if (level2InviterId == null) {
            return;
        }

        PromoterLevel level2Promoter = promoterLevelService.getOrCreatePromoterLevel(level2InviterId);

        BigDecimal l2Amount;
        switch (level2Promoter.getLevel()) {
            case 3:  l2Amount = new BigDecimal("8"); break;
            case 4:  l2Amount = new BigDecimal("10"); break;
            default: l2Amount = new BigDecimal("5"); break;
        }

        int commissionType;
        if (isRenewal) {
            if (level2Promoter.getLevel() >= 2) {
                l2Amount = new BigDecimal("5");
                commissionType = 4;
            } else {
                return;
            }
        } else {
            commissionType = 3;
        }

        if (!checkMonthlyLimit(level2InviterId, l2Amount, 2)) {
            log.info("二级返佣超月封顶: inviterId={}, amount={}", level2InviterId, l2Amount);
            return;
        }

        MemberOrder order = memberOrderMapper.selectById(orderId);
        Commission level2Commission = new Commission();
        level2Commission.setUserId(level2InviterId);
        level2Commission.setOrderId(orderId);
        level2Commission.setInviteeId(payer.getId());
        level2Commission.setLevel(2);
        level2Commission.setCommissionType(commissionType);
        level2Commission.setOrderAmount(order.getPayAmount());
        level2Commission.setCommissionRate(l2Amount);
        level2Commission.setCommissionAmount(l2Amount);
        level2Commission.setStatus(0);
        level2Commission.setCreatedAt(LocalDateTime.now());

        String signPayload = buildCommissionSignPayload(level2Commission);
        level2Commission.setDataSign(dataSignService.sign(signPayload));

        commissionMapper.insert(level2Commission);
        addFrozenBalance(level2InviterId, l2Amount);

        log.info("二级返佣: inviterId={}, amount={}, type={}", level2InviterId, l2Amount, commissionType);
    }

    private boolean checkMonthlyLimit(Long userId, BigDecimal newAmount, int level) {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        if (level == 2) {
            BigDecimal monthlyLevel2 = getMonthlyCommission(userId, monthStart, Arrays.asList(3, 4));
            if (monthlyLevel2.add(newAmount).compareTo(new BigDecimal("500")) > 0) {
                return false;
            }
        }

        BigDecimal monthlyTotal = getMonthlyCommission(userId, monthStart, null);
        return monthlyTotal.add(newAmount).compareTo(new BigDecimal("5000")) <= 0;
    }

    private BigDecimal getMonthlyCommission(Long userId, LocalDateTime monthStart, List<Integer> commissionTypes) {
        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<Commission>()
                .eq(Commission::getUserId, userId)
                .ge(Commission::getCreatedAt, monthStart)
                .in(Commission::getStatus, 0, 1);
        if (commissionTypes != null && !commissionTypes.isEmpty()) {
            wrapper.in(Commission::getCommissionType, commissionTypes);
        }
        List<Commission> commissions = commissionMapper.selectList(wrapper);
        return commissions.stream()
                .map(Commission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(rollbackFor = Exception.class)
    public void settleCommission(Long commissionId) {
        Commission commission = commissionMapper.selectById(commissionId);
        if (commission == null || commission.getStatus() != 0) {
            return;
        }

        int affected = commissionMapper.update(null,
                new LambdaUpdateWrapper<Commission>()
                        .eq(Commission::getId, commissionId)
                        .eq(Commission::getStatus, 0)
                        .set(Commission::getStatus, 1)
                        .set(Commission::getSettleTime, LocalDateTime.now()));
        if (affected == 0) {
            return;
        }

        BigDecimal amount = commission.getCommissionAmount();
        promoterLevelMapper.settleFrozenToAvailable(commission.getUserId(), amount);

        log.info("返佣结算: commissionId={}, userId={}, amount={}",
                commissionId, commission.getUserId(), amount);
    }

    public List<CommissionVO> listCommissions(Long userId) {
        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getUserId, userId)
                        .orderByDesc(Commission::getCreatedAt)
        );

        List<CommissionVO> list = new ArrayList<>();
        for (Commission c : commissions) {
            User invitee = userMapper.selectById(c.getInviteeId());
            CommissionVO vo = new CommissionVO();
            vo.setId(c.getId());
            vo.setInviteeId(c.getInviteeId());
            vo.setInviteeNickname(invitee != null ? invitee.getNickname() : null);
            vo.setLevel(c.getLevel());
            vo.setCommissionType(c.getCommissionType());
            vo.setOrderAmount(c.getOrderAmount());
            vo.setCommissionRate(c.getCommissionRate());
            vo.setCommissionAmount(c.getCommissionAmount());
            vo.setStatus(c.getStatus());
            vo.setStatusName(getCommissionStatusName(c.getStatus()));
            vo.setSettleTime(c.getSettleTime());
            vo.setCreatedAt(c.getCreatedAt());
            list.add(vo);
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeCommission(Long orderId) {
        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getOrderId, orderId)
                        .in(Commission::getStatus, 0, 1)
        );

        for (Commission c : commissions) {
            int originalStatus = c.getStatus();
            c.setStatus(2);
            commissionMapper.updateById(c);

            BigDecimal amount = c.getCommissionAmount();
            if (originalStatus == 0) {
                promoterLevelMapper.decrementFrozenBalance(c.getUserId(), amount);
            } else {
                promoterLevelMapper.decrementAvailableBalance(c.getUserId(), amount);
            }

            log.info("返佣撤销: commissionId={}, userId={}", c.getId(), c.getUserId());
        }
    }

    private void addFrozenBalance(Long userId, BigDecimal amount) {
        promoterLevelMapper.incrementFrozenBalance(userId, amount);
    }

    private void updateInvitationStatus(Long inviterId, Long inviteeId) {
        invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getInviterId, inviterId)
                .eq(Invitation::getInviteeId, inviteeId)
                .set(Invitation::getInviteeStatus, 1)
        );
    }

    private String buildCommissionSignPayload(Commission c) {
        return c.getId() + "|" + c.getUserId() + "|" + c.getCommissionAmount() + "|"
                + c.getCommissionType() + "|" + c.getOrderId() + "|" + c.getInviteeId()
                + "|" + c.getStatus() + "|" + c.getCreatedAt();
    }

    private String getCommissionStatusName(Integer status) {
        return switch (status) {
            case 0 -> "冻结中";
            case 1 -> "已结算";
            case 2 -> "已撤销";
            default -> "未知";
        };
    }
}
