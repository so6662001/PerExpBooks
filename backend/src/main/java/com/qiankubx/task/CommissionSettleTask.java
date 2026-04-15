package com.qiankubx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.module.member.entity.MemberOrder;
import com.qiankubx.module.member.mapper.MemberOrderMapper;
import com.qiankubx.module.promotion.entity.Commission;
import com.qiankubx.module.promotion.mapper.CommissionMapper;
import com.qiankubx.module.promotion.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommissionSettleTask {

    private final CommissionMapper commissionMapper;
    private final MemberOrderMapper memberOrderMapper;
    private final CommissionService commissionService;

    @Scheduled(cron = "0 30 * * * ?")
    public void settleCommissions() {
        log.info("[定时任务] 检查待结算返佣...");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<Commission> pendingCommissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getStatus, 0)
        );

        int settled = 0;
        for (Commission commission : pendingCommissions) {
            MemberOrder order = memberOrderMapper.selectById(commission.getOrderId());
            if (order != null && order.getPayTime() != null && order.getPayTime().isBefore(sevenDaysAgo)) {
                if (order.getRefundStatus() == null || order.getRefundStatus() == 0) {
                    try {
                        commissionService.settleCommission(commission.getId());
                        settled++;
                    } catch (Exception e) {
                        log.error("[定时任务] 返佣结算失败: commissionId={}", commission.getId(), e);
                    }
                }
            }
        }
        log.info("[定时任务] 返佣结算完成, 结算数量: {}", settled);
    }
}
