package com.qiankubx.task;

import com.qiankubx.module.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataReconcileTask {

    private final AuditService auditService;

    @Scheduled(cron = "0 0 * * * ?")
    public void reconcile() {
        log.info("[定时任务] 执行数据对账...");
        try {
            Map<String, Object> report = auditService.reconcileAll();
            log.info("[定时任务] 数据对账完成, 状态: {}", report.get("status"));
        } catch (Exception e) {
            log.error("[定时任务] 数据对账失败", e);
        }
    }
}
