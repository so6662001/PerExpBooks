package com.qiankubx.module.promotion.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.config.RabbitMQConfig;
import com.qiankubx.module.promotion.entity.Commission;
import com.qiankubx.module.promotion.mapper.CommissionMapper;
import com.qiankubx.module.promotion.service.CommissionService;
import com.qiankubx.module.promotion.service.PointsService;
import com.qiankubx.module.promotion.service.PromoterLevelService;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final CommissionMapper commissionMapper;

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
            User payer = userMapper.selectById(userId);
            if (payer != null && payer.getInviterId() != null) {
                boolean isFirst = commissionMapper.selectCount(
                        new LambdaQueryWrapper<Commission>()
                                .eq(Commission::getUserId, payer.getInviterId())
                                .eq(Commission::getInviteeId, userId)
                                .in(Commission::getCommissionType, 1, 3)
                ) <= 1;

                if (isFirst) {
                    pointsService.addPoints(payer.getInviterId(), 20, "invite_paid", orderId, "邀请好友首次付费");
                } else {
                    pointsService.addPoints(payer.getInviterId(), 10, "invite_renewal", orderId, "邀请好友续费");
                }

                if (payer.getRootInviterId() != null) {
                    pointsService.addPoints(payer.getRootInviterId(), 5, "invite_paid_l2", orderId, "二级好友付费");
                }
            }
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
