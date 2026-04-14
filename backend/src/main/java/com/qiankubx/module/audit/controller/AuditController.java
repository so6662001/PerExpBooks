package com.qiankubx.module.audit.controller;

import com.qiankubx.common.response.Result;
import com.qiankubx.module.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    public Result<Map<String, Object>> logs(
            @RequestParam(required = false) String targetTable,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(auditService.queryLogs(targetTable, targetId, operatorId, startDate, endDate, page, pageSize));
    }

    @PostMapping("/reconcile")
    public Result<Map<String, Object>> reconcile() {
        return Result.ok(auditService.reconcileAll());
    }

    @GetMapping("/reconcile-report")
    public Result<Map<String, Object>> reconcileReport() {
        return Result.ok(auditService.getReconcileReport());
    }

    @GetMapping("/tamper-alerts")
    public Result<List<Map<String, Object>>> tamperAlerts() {
        return Result.ok(auditService.getTamperAlerts());
    }

    @PostMapping("/verify-sign/{table}/{id}")
    public Result<Map<String, Object>> verifySign(@PathVariable String table, @PathVariable Long id) {
        return Result.ok(auditService.verifySign(table, id));
    }

    @PostMapping("/verify-chain/{userId}")
    public Result<Map<String, Object>> verifyChain(@PathVariable Long userId) {
        return Result.ok(auditService.verifyChain(userId));
    }

    @GetMapping("/anomaly-users")
    public Result<List<Map<String, Object>>> anomalyUsers() {
        return Result.ok(auditService.getAnomalyUsers());
    }
}
