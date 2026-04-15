package com.qiankubx.common.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Around("execution(* com.qiankubx.module.promotion.service.PointsService.addPoints(..)) || " +
            "execution(* com.qiankubx.module.promotion.service.CommissionService.triggerCommission(..)) || " +
            "execution(* com.qiankubx.module.promotion.service.WithdrawalService.applyWithdraw(..)) || " +
            "execution(* com.qiankubx.module.member.service.MemberService.handlePaySuccess(..)) || " +
            "execution(* com.qiankubx.module.expense.service.ExpenseService.createExpense(..)) || " +
            "execution(* com.qiankubx.module.expense.service.ExpenseService.updateExpense(..)) || " +
            "execution(* com.qiankubx.module.reimbursement.service.ReimbursementService.generate(..)) || " +
            "execution(* com.qiankubx.module.reimbursement.service.ReimbursementService.confirmReceived(..))")
    public Object auditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        Long operatorId = 0L;
        if (args.length > 0 && args[0] instanceof Long) {
            operatorId = (Long) args[0];
        }

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        try {
            String action = methodName.contains("create") || methodName.contains("add") || methodName.contains("generate")
                    ? "insert" : "update";
            auditLogService.log(
                    operatorId, "system",
                    className, 0L,
                    action, methodName, null,
                    "duration=" + duration + "ms",
                    null, null, java.util.UUID.randomUUID().toString()
            );
        } catch (Exception e) {
            log.warn("审计日志记录失败", e);
        }

        return result;
    }
}
