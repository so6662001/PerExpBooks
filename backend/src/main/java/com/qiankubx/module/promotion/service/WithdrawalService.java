package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.common.security.TamperAlertService;
import com.qiankubx.common.util.AesUtil;
import com.qiankubx.module.promotion.dto.BalanceVO;
import com.qiankubx.module.promotion.dto.WithdrawalVO;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.entity.Withdrawal;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import com.qiankubx.module.promotion.mapper.WithdrawalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalMapper withdrawalMapper;
    private final PromoterLevelMapper promoterLevelMapper;
    private final PromoterLevelService promoterLevelService;
    private final DataSignService dataSignService;
    private final AntiCheatService antiCheatService;
    private final TamperAlertService tamperAlertService;
    private final AesUtil aesUtil;

    @Transactional(rollbackFor = Exception.class)
    public void applyWithdraw(Long userId, BigDecimal amount, Integer withdrawType, String accountInfo) {
        if (amount.compareTo(new BigDecimal("50")) < 0) {
            throw new BizException(400, "最低提现金额为50元");
        }

        PromoterLevel level = promoterLevelService.getOrCreatePromoterLevel(userId);
        if (level.getAvailableBalance().compareTo(amount) < 0) {
            throw new BizException(400, "可提现余额不足");
        }

        if (tamperAlertService.isUserFrozen(userId)) {
            throw new BizException(403, "您的账户资金操作已被暂时冻结，请联系客服");
        }

        antiCheatService.assertWithdrawalNotFlagged(userId);

        LocalDateTime monthStart = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        Long monthCount = withdrawalMapper.selectCount(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getUserId, userId)
                        .ge(Withdrawal::getCreatedAt, monthStart)
                        .ne(Withdrawal::getStatus, 2)
        );
        if (monthCount >= 2) {
            throw new BizException(400, "每月最多提现2次");
        }

        level.setAvailableBalance(level.getAvailableBalance().subtract(amount));
        level.setWithdrawnAmount(level.getWithdrawnAmount().add(amount));
        level.setUpdatedAt(LocalDateTime.now());
        level.setDataSign(dataSignService.sign(promoterLevelService.buildPromoterSignPayload(level)));

        promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                .eq(PromoterLevel::getUserId, userId)
                .setSql("available_balance = available_balance - " + amount)
                .setSql("withdrawn_amount = withdrawn_amount + " + amount)
                .set(PromoterLevel::getUpdatedAt, level.getUpdatedAt())
                .set(PromoterLevel::getDataSign, level.getDataSign())
        );

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setAmount(amount);
        withdrawal.setWithdrawType(withdrawType);
        withdrawal.setAccountInfo(aesUtil.encrypt(accountInfo));
        withdrawal.setStatus(0);
        withdrawal.setCreatedAt(LocalDateTime.now());

        String signPayload = withdrawal.getId() + "|" + userId + "|" + amount + "|" + withdrawal.getStatus() + "|" + withdrawal.getCreatedAt();
        withdrawal.setDataSign(dataSignService.sign(signPayload));

        withdrawalMapper.insert(withdrawal);

        log.info("提现申请: userId={}, amount={}, type={}", userId, amount, withdrawType);
    }

    public List<WithdrawalVO> listWithdrawals(Long userId) {
        List<Withdrawal> list = withdrawalMapper.selectList(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getUserId, userId)
                        .orderByDesc(Withdrawal::getCreatedAt)
        );
        return list.stream().map(this::toVO).toList();
    }

    public BalanceVO getBalance(Long userId) {
        PromoterLevel level = promoterLevelService.getOrCreatePromoterLevel(userId);

        BalanceVO vo = new BalanceVO();
        vo.setAvailableBalance(level.getAvailableBalance());
        vo.setFrozenBalance(level.getFrozenBalance());
        vo.setTotalCommission(level.getTotalCommission());
        vo.setWithdrawnAmount(level.getWithdrawnAmount());
        return vo;
    }

    private WithdrawalVO toVO(Withdrawal w) {
        WithdrawalVO vo = new WithdrawalVO();
        vo.setId(w.getId());
        vo.setAmount(w.getAmount());
        vo.setWithdrawType(w.getWithdrawType());
        vo.setWithdrawTypeName(switch (w.getWithdrawType()) {
            case 1 -> "微信";
            case 2 -> "支付宝";
            case 3 -> "银行卡";
            default -> "未知";
        });
        vo.setStatus(w.getStatus());
        vo.setStatusName(switch (w.getStatus()) {
            case 0 -> "处理中";
            case 1 -> "已完成";
            case 2 -> "已拒绝";
            default -> "未知";
        });
        vo.setRejectReason(w.getRejectReason());
        vo.setAccountInfo(aesUtil.decrypt(w.getAccountInfo()));
        vo.setProcessedAt(w.getProcessedAt());
        vo.setCreatedAt(w.getCreatedAt());
        return vo;
    }
}
