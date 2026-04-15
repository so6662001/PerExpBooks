package com.qiankubx.task;

import com.qiankubx.common.security.TamperAlertService;
import com.qiankubx.module.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataReconcileTask {

    private final AuditService auditService;
    private final TamperAlertService tamperAlertService;

    @Scheduled(cron = "0 0 * * * ?")
    @SuppressWarnings("unchecked")
    public void reconcile() {
        log.info("[定时任务] 执行数据对账...");
        try {
            Map<String, Object> report = auditService.reconcileAll();
            String status = (String) report.get("status");
            log.info("[定时任务] 数据对账完成, 状态: {}", status);

            if ("FAIL".equals(status)) {
                List<Map<String, Object>> anomalies = (List<Map<String, Object>>) report.get("anomalies");
                if (anomalies != null) {
                    for (Map<String, Object> anomaly : anomalies) {
                        Long userId = ((Number) anomaly.get("userId")).longValue();
                        String type = (String) anomaly.get("type");
                        tamperAlertService.alertUrgent("reconcile_" + type,
                                "数据对账异常: " + type, userId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[定时任务] 数据对账失败", e);
        }
    }
}
