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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        BigDecimal level1Amount = order.getPayAmount()
                .multiply(level1Promoter.getLevel1Rate())
                .setScale(2, RoundingMode.HALF_UP);

        if (level1Amount.compareTo(BigDecimal.ZERO) > 0) {
            Commission level1Commission = new Commission();
            level1Commission.setUserId(level1InviterId);
            level1Commission.setOrderId(orderId);
            level1Commission.setInviteeId(payerId);
            level1Commission.setLevel(1);
            level1Commission.setOrderAmount(order.getPayAmount());
            level1Commission.setCommissionRate(level1Promoter.getLevel1Rate());
            level1Commission.setCommissionAmount(level1Amount);
            level1Commission.setStatus(0);
            level1Commission.setCreatedAt(LocalDateTime.now());

            String signPayload = buildCommissionSignPayload(level1Commission);
            level1Commission.setDataSign(dataSignService.sign(signPayload));

            commissionMapper.insert(level1Commission);
            addFrozenBalance(level1InviterId, level1Amount);

            log.info("一级返佣: inviterId={}, amount={}", level1InviterId, level1Amount);
        }

        updateInvitationStatus(level1InviterId, payerId);
        promoterLevelService.incrementPaidInviteCount(level1InviterId);
        promoterLevelService.checkAndUpgrade(level1InviterId);

        Long level2InviterId = payer.getRootInviterId();
        if (level2InviterId != null) {
            PromoterLevel level2Promoter = promoterLevelService.getOrCreatePromoterLevel(level2InviterId);

            BigDecimal level2Amount = order.getPayAmount()
                    .multiply(level2Promoter.getLevel2Rate())
                    .setScale(2, RoundingMode.HALF_UP);

            if (level2Amount.compareTo(BigDecimal.ZERO) > 0) {
                Commission level2Commission = new Commission();
                level2Commission.setUserId(level2InviterId);
                level2Commission.setOrderId(orderId);
                level2Commission.setInviteeId(payerId);
                level2Commission.setLevel(2);
                level2Commission.setOrderAmount(order.getPayAmount());
                level2Commission.setCommissionRate(level2Promoter.getLevel2Rate());
                level2Commission.setCommissionAmount(level2Amount);
                level2Commission.setStatus(0);
                level2Commission.setCreatedAt(LocalDateTime.now());

                String signPayload = buildCommissionSignPayload(level2Commission);
                level2Commission.setDataSign(dataSignService.sign(signPayload));

                commissionMapper.insert(level2Commission);
                addFrozenBalance(level2InviterId, level2Amount);

                log.info("二级返佣: inviterId={}, amount={}", level2InviterId, level2Amount);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void settleCommission(Long commissionId) {
        Commission commission = commissionMapper.selectById(commissionId);
        if (commission == null || commission.getStatus() != 0) {
            return;
        }

        commission.setStatus(1);
        commission.setSettleTime(LocalDateTime.now());
        commissionMapper.updateById(commission);

        BigDecimal amount = commission.getCommissionAmount();
        promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                .eq(PromoterLevel::getUserId, commission.getUserId())
                .setSql("frozen_balance = GREATEST(frozen_balance - " + amount + ", 0)")
                .setSql("available_balance = available_balance + " + amount)
                .setSql("total_commission = total_commission + " + amount)
                .set(PromoterLevel::getUpdatedAt, LocalDateTime.now())
        );

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
                promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                        .eq(PromoterLevel::getUserId, c.getUserId())
                        .setSql("frozen_balance = GREATEST(frozen_balance - " + amount + ", 0)")
                        .set(PromoterLevel::getUpdatedAt, LocalDateTime.now())
                );
            } else {
                promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                        .eq(PromoterLevel::getUserId, c.getUserId())
                        .setSql("available_balance = GREATEST(available_balance - " + amount + ", 0)")
                        .set(PromoterLevel::getUpdatedAt, LocalDateTime.now())
                );
            }

            log.info("返佣撤销: commissionId={}, userId={}", c.getId(), c.getUserId());
        }
    }

    private void addFrozenBalance(Long userId, BigDecimal amount) {
        promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                .eq(PromoterLevel::getUserId, userId)
                .setSql("frozen_balance = frozen_balance + " + amount)
                .set(PromoterLevel::getUpdatedAt, LocalDateTime.now())
        );
    }

    private void updateInvitationStatus(Long inviterId, Long inviteeId) {
        invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getInviterId, inviterId)
                .eq(Invitation::getInviteeId, inviteeId)
                .set(Invitation::getInviteeStatus, 1)
        );
    }

    private String buildCommissionSignPayload(Commission c) {
        return c.getUserId() + "|"
                + c.getOrderId() + "|"
                + c.getInviteeId() + "|"
                + c.getLevel() + "|"
                + c.getCommissionAmount();
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
