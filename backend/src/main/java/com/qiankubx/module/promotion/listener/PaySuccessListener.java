package com.qiankubx.module.promotion.listener;

import com.qiankubx.common.config.RabbitMQConfig;
import com.qiankubx.module.promotion.service.CommissionService;
import com.qiankubx.module.promotion.service.PointsService;
import com.qiankubx.module.promotion.service.PromoterLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaySuccessListener {

    private final CommissionService commissionService;
    private final PointsService pointsService;
    private final PromoterLevelService promoterLevelService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAY_SUCCESS)
    public void onMessage(Map<String, Object> message) {
        Long orderId = message.get("orderId") != null ? ((Number) message.get("orderId")).longValue() : null;
        Long userId = message.get("userId") != null ? ((Number) message.get("userId")).longValue() : null;

        log.info("[MQ] 接收支付成功消息: orderId={}, userId={}", orderId, userId);

        if (orderId == null || userId == null) {
            log.warn("[MQ] 消息参数不完整, 跳过");
            return;
        }

        try {
            commissionService.triggerCommission(orderId);
            log.info("[MQ] 返佣计算完成: orderId={}", orderId);
        } catch (Exception e) {
            log.error("[MQ] 返佣计算失败: orderId={}", orderId, e);
        }

        try {
            pointsService.addPoints(userId, 100, "purchase", orderId, "购买会员奖励积分");
            log.info("[MQ] 积分增加完成: userId={}", userId);
        } catch (Exception e) {
            log.error("[MQ] 积分增加失败: userId={}", userId, e);
        }

        try {
            promoterLevelService.checkAndUpgrade(userId);
            log.info("[MQ] 推广等级检查完成: userId={}", userId);
        } catch (Exception e) {
            log.error("[MQ] 推广等级检查失败: userId={}", userId, e);
        }
    }
}
