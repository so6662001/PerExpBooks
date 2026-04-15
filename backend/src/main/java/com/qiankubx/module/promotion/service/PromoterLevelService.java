package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.promotion.dto.LevelInfoVO;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoterLevelService {

    private final PromoterLevelMapper promoterLevelMapper;
    private final DataSignService dataSignService;

    public PromoterLevel getOrCreatePromoterLevel(Long userId) {
        PromoterLevel level = promoterLevelMapper.selectOne(
                new LambdaQueryWrapper<PromoterLevel>()
                        .eq(PromoterLevel::getUserId, userId)
        );
        if (level == null) {
            level = new PromoterLevel();
            level.setUserId(userId);
            level.setLevel(1);
            level.setLevelName("新手推广员");
            level.setLevel1Rate(new BigDecimal("19"));
            level.setLevel2Rate(new BigDecimal("5"));
            level.setInviteCount(0);
            level.setPaidInviteCount(0);
            level.setTotalCommission(BigDecimal.ZERO);
            level.setAvailableBalance(BigDecimal.ZERO);
            level.setFrozenBalance(BigDecimal.ZERO);
            level.setWithdrawnAmount(BigDecimal.ZERO);
            level.setPoints(0);
            level.setTotalPoints(0);
            level.setCreatedAt(LocalDateTime.now());
            level.setUpdatedAt(LocalDateTime.now());
            level.setDataSign(dataSignService.sign(buildPromoterSignPayload(level)));
            promoterLevelMapper.insert(level);
        }
        return level;
    }

    public LevelInfoVO getLevelInfo(Long userId) {
        PromoterLevel level = getOrCreatePromoterLevel(userId);

        LevelInfoVO vo = new LevelInfoVO();
        vo.setLevel(level.getLevel());
        vo.setLevelName(level.getLevelName());
        vo.setLevel1Rate(level.getLevel1Rate());
        vo.setLevel2Rate(level.getLevel2Rate());
        vo.setInviteCount(level.getInviteCount());
        vo.setPaidInviteCount(level.getPaidInviteCount());
        vo.setTotalCommission(level.getTotalCommission());
        vo.setPoints(level.getPoints());
        vo.setTotalPoints(level.getTotalPoints());

        int nextRequired = getNextLevelInviteRequired(level.getLevel());
        vo.setNextLevelInviteRequired(nextRequired > 0 ? nextRequired : null);
        vo.setNextLevelName(getNextLevelName(level.getLevel()));

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkAndUpgrade(Long userId) {
        PromoterLevel level = getOrCreatePromoterLevel(userId);
        int inviteCount = level.getPaidInviteCount();

        int newLevel;
        String newLevelName;
        BigDecimal newLevel1Rate;
        BigDecimal newLevel2Rate;

        if (inviteCount >= 30) {
            newLevel = 4;
            newLevelName = "钻石推广员";
            newLevel1Rate = new BigDecimal("25");
            newLevel2Rate = new BigDecimal("10");
        } else if (inviteCount >= 10) {
            newLevel = 3;
            newLevelName = "金牌推广员";
            newLevel1Rate = new BigDecimal("22");
            newLevel2Rate = new BigDecimal("8");
        } else if (inviteCount >= 3) {
            newLevel = 2;
            newLevelName = "银牌推广员";
            newLevel1Rate = new BigDecimal("19");
            newLevel2Rate = new BigDecimal("5");
        } else {
            newLevel = 1;
            newLevelName = "新手推广员";
            newLevel1Rate = new BigDecimal("19");
            newLevel2Rate = new BigDecimal("5");
        }

        if (newLevel > level.getLevel()) {
            level.setLevel(newLevel);
            level.setLevelName(newLevelName);
            level.setLevel1Rate(newLevel1Rate);
            level.setLevel2Rate(newLevel2Rate);
            level.setUpdatedAt(LocalDateTime.now());
            level.setDataSign(dataSignService.sign(buildPromoterSignPayload(level)));

            promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                    .eq(PromoterLevel::getUserId, userId)
                    .set(PromoterLevel::getLevel, newLevel)
                    .set(PromoterLevel::getLevelName, newLevelName)
                    .set(PromoterLevel::getLevel1Rate, newLevel1Rate)
                    .set(PromoterLevel::getLevel2Rate, newLevel2Rate)
                    .set(PromoterLevel::getUpdatedAt, level.getUpdatedAt())
                    .set(PromoterLevel::getDataSign, level.getDataSign())
            );
            log.info("推广员升级: userId={}, {} -> {}", userId, level.getLevelName(), newLevelName);
        }
    }

    public void incrementInviteCount(Long userId) {
        PromoterLevel level = getOrCreatePromoterLevel(userId);
        level.setInviteCount(level.getInviteCount() + 1);
        level.setUpdatedAt(LocalDateTime.now());
        level.setDataSign(dataSignService.sign(buildPromoterSignPayload(level)));

        promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                .eq(PromoterLevel::getUserId, userId)
                .setSql("invite_count = invite_count + 1")
                .set(PromoterLevel::getUpdatedAt, level.getUpdatedAt())
                .set(PromoterLevel::getDataSign, level.getDataSign())
        );
    }

    public void incrementPaidInviteCount(Long userId) {
        PromoterLevel level = getOrCreatePromoterLevel(userId);
        level.setPaidInviteCount(level.getPaidInviteCount() + 1);
        level.setUpdatedAt(LocalDateTime.now());
        level.setDataSign(dataSignService.sign(buildPromoterSignPayload(level)));

        promoterLevelMapper.update(null, new LambdaUpdateWrapper<PromoterLevel>()
                .eq(PromoterLevel::getUserId, userId)
                .setSql("paid_invite_count = paid_invite_count + 1")
                .set(PromoterLevel::getUpdatedAt, level.getUpdatedAt())
                .set(PromoterLevel::getDataSign, level.getDataSign())
        );
    }

    public String buildPromoterSignPayload(PromoterLevel pl) {
        return pl.getUserId() + "|" + pl.getPoints() + "|" + pl.getTotalPoints() + "|"
                + pl.getAvailableBalance() + "|" + pl.getFrozenBalance() + "|" + pl.getWithdrawnAmount()
                + "|" + pl.getTotalCommission() + "|" + pl.getLevel() + "|" + pl.getUpdatedAt();
    }

    private int getNextLevelInviteRequired(int currentLevel) {
        return switch (currentLevel) {
            case 1 -> 3;
            case 2 -> 10;
            case 3 -> 30;
            default -> -1;
        };
    }

    private String getNextLevelName(int currentLevel) {
        return switch (currentLevel) {
            case 1 -> "银牌推广员";
            case 2 -> "金牌推广员";
            case 3 -> "钻石推广员";
            default -> null;
        };
    }
}
