package com.qiankubx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.mapper.AgreementVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementEffectiveTask {

    private final AgreementVersionMapper agreementVersionMapper;

    @Scheduled(cron = "0 0 * * * ?")
    public void checkAndActivate() {
        log.info("[定时任务] 检查待生效协议...");
        List<AgreementVersion> pending = agreementVersionMapper.selectList(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getStatus, 0)
                        .le(AgreementVersion::getEffectiveAt, LocalDateTime.now())
                        .isNotNull(AgreementVersion::getEffectiveAt)
        );

        for (AgreementVersion version : pending) {
            version.setStatus(1);
            version.setUpdatedAt(LocalDateTime.now());
            agreementVersionMapper.updateById(version);
            log.info("[定时任务] 协议已激活: id={}, type={}, versionCode={}", version.getId(), version.getType(), version.getVersionCode());
        }
        log.info("[定时任务] 协议激活检查完成, 激活数量: {}", pending.size());
    }
}
