package com.qiankubx.task;

import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.mapper.PromoterLevelMapper;
import com.qiankubx.module.promotion.service.PromoterLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromoterLevelUpgradeTask {

    private final PromoterLevelMapper promoterLevelMapper;
    private final PromoterLevelService promoterLevelService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void checkUpgrade() {
        log.info("[定时任务] 检查推广等级升级...");
        List<PromoterLevel> allLevels = promoterLevelMapper.selectList(null);
        int upgraded = 0;
        for (PromoterLevel level : allLevels) {
            try {
                int currentLevel = level.getLevel();
                promoterLevelService.checkAndUpgrade(level.getUserId());
                PromoterLevel updated = promoterLevelMapper.selectById(level.getId());
                if (updated != null && updated.getLevel() > currentLevel) {
                    upgraded++;
                }
            } catch (Exception e) {
                log.error("[定时任务] 等级升级检查失败: userId={}", level.getUserId(), e);
            }
        }
        log.info("[定时任务] 推广等级升级检查完成, 升级数量: {}", upgraded);
    }
}
