package com.qiankubx.module.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SceneTriggerService {

    public Map<String, Object> checkTriggers(Long userId, String scene) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene", scene);
        result.put("triggered", false);

        switch (scene) {
            case "INVOICE_PARSED" -> {
                result.put("triggered", true);
                result.put("guideType", "share");
                result.put("title", "发票解析成功");
                result.put("message", "分享给同事，一起高效报销");
                result.put("buttonText", "立即分享");
                result.put("action", "share_invite");
            }
            case "REIMBURSEMENT_DONE" -> {
                result.put("triggered", true);
                result.put("guideType", "achievement_card");
                result.put("title", "报销单已生成");
                result.put("message", "分享您的报销战绩卡片到朋友圈");
                result.put("buttonText", "生成战绩卡");
                result.put("action", "generate_card");
            }
            case "RECEIVED" -> {
                result.put("triggered", true);
                result.put("guideType", "invite");
                result.put("title", "已确认收款");
                result.put("message", "邀请好友使用钱酷，您可获得返佣奖励");
                result.put("buttonText", "邀请好友");
                result.put("action", "invite_friend");
            }
            case "QUOTA_LOW" -> {
                result.put("triggered", true);
                result.put("guideType", "upgrade");
                result.put("title", "额度即将用尽");
                result.put("message", "升级会员享无限额度，或邀请好友获取额外额度");
                result.put("buttonText", "查看方案");
                result.put("action", "view_upgrade");
            }
            case "MONTHLY_STATS" -> {
                result.put("triggered", true);
                result.put("guideType", "monthly_report");
                result.put("title", "月度报告已生成");
                result.put("message", "分享您的月度费用报告");
                result.put("buttonText", "查看报告");
                result.put("action", "view_monthly_report");
            }
            default -> log.debug("未知场景: {}", scene);
        }

        return result;
    }
}
