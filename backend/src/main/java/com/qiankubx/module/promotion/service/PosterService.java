package com.qiankubx.module.promotion.service;

import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosterService {

    private final UserMapper userMapper;
    private final InviteService inviteService;

    public Map<String, Object> generateInvitePoster(Long userId) {
        User user = userMapper.selectById(userId);
        String inviteCode = inviteService.getInviteCode(userId).getInviteCode();

        Map<String, Object> posterConfig = new LinkedHashMap<>();
        posterConfig.put("type", "invite");
        posterConfig.put("backgroundColor", "#4A90D9");
        posterConfig.put("title", "钱酷报销 - 智能费用管理");
        posterConfig.put("subtitle", (user != null ? user.getNickname() : "好友") + " 邀请你体验");
        posterConfig.put("inviteCode", inviteCode);
        posterConfig.put("qrCodeUrl", "https://qianku.com/register?invite=" + inviteCode);
        posterConfig.put("avatarUrl", user != null ? user.getAvatarUrl() : null);
        posterConfig.put("nickname", user != null ? user.getNickname() : null);
        posterConfig.put("slogan", "告别报销烦恼，发票秒变报销单");
        posterConfig.put("features", java.util.List.of("AI发票识别", "一键生成报销单", "邮件直达财务"));
        return posterConfig;
    }

    public Map<String, Object> generateSocialCard(Long userId, String cardType) {
        User user = userMapper.selectById(userId);
        Map<String, Object> cardConfig = new LinkedHashMap<>();
        cardConfig.put("type", cardType);
        cardConfig.put("nickname", user != null ? user.getNickname() : null);
        cardConfig.put("avatarUrl", user != null ? user.getAvatarUrl() : null);

        switch (cardType != null ? cardType : "") {
            case "achievement" -> {
                cardConfig.put("backgroundColor", "#FF6B35");
                cardConfig.put("title", "报销达人");
                cardConfig.put("subtitle", "我的报销战绩");
            }
            case "monthly_report" -> {
                cardConfig.put("backgroundColor", "#2E86AB");
                cardConfig.put("title", "月度报告");
                cardConfig.put("subtitle", "本月费用一览");
            }
            case "savings" -> {
                cardConfig.put("backgroundColor", "#28A745");
                cardConfig.put("title", "省钱达人");
                cardConfig.put("subtitle", "使用钱酷省了这么多时间");
            }
            default -> {
                cardConfig.put("backgroundColor", "#6C757D");
                cardConfig.put("title", "钱酷报销");
                cardConfig.put("subtitle", "智能费用管理");
            }
        }

        cardConfig.put("qrCodeUrl", "https://qianku.com/register?invite=" +
                (user != null && user.getInviteCode() != null ? user.getInviteCode() : ""));
        return cardConfig;
    }
}
