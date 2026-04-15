package com.qiankubx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.entity.UserAgreementSign;
import com.qiankubx.module.agreement.mapper.AgreementVersionMapper;
import com.qiankubx.module.agreement.mapper.UserAgreementSignMapper;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementRemindTask {

    private final AgreementVersionMapper agreementVersionMapper;
    private final UserAgreementSignMapper userAgreementSignMapper;
    private final UserMapper userMapper;

    @Scheduled(cron = "0 0 9 * * ?")
    public void remindUnsignedUsers() {
        log.info("[定时任务] 检查重大变更未签署用户...");
        List<AgreementVersion> majorVersions = agreementVersionMapper.selectList(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getStatus, 1)
                        .eq(AgreementVersion::getChangeLevel, "major")
                        .le(AgreementVersion::getEffectiveAt, LocalDateTime.now())
        );

        for (AgreementVersion version : majorVersions) {
            Long signedCount = userAgreementSignMapper.selectCount(
                    new LambdaQueryWrapper<UserAgreementSign>()
                            .eq(UserAgreementSign::getVersionId, version.getId())
            );
            Long totalUsers = userMapper.selectCount(null);
            long unsigned = Math.max(0, totalUsers - signedCount);
            if (unsigned > 0) {
                log.info("[定时任务] 协议提醒: versionId={}, type={}, 未签署用户数={}",
                        version.getId(), version.getType(), unsigned);
            }
        }
        log.info("[定时任务] 协议提醒检查完成");
    }
}
