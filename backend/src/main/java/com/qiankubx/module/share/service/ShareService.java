package com.qiankubx.module.share.service;

import com.qiankubx.module.share.dto.ShareLogDTO;
import com.qiankubx.module.share.entity.ShareLog;
import com.qiankubx.module.share.mapper.ShareLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareLogMapper shareLogMapper;

    public void logShare(Long userId, ShareLogDTO dto) {
        ShareLog shareLog = new ShareLog();
        shareLog.setUserId(userId);
        shareLog.setShareType(dto.getShareType());
        shareLog.setContentType(dto.getContentType());
        shareLog.setShareScene(dto.getShareScene());
        shareLog.setClickCount(0);
        shareLog.setRegisterCount(0);
        shareLog.setCreatedAt(LocalDateTime.now());

        shareLogMapper.insert(shareLog);
    }

    public List<Map<String, String>> getShareTemplates() {
        List<Map<String, String>> templates = new ArrayList<>();

        Map<String, String> t1 = new HashMap<>();
        t1.put("type", "效率型");
        t1.put("title", "告别手动记账，发票扫一扫自动录入");
        t1.put("desc", "钱酷报销，让报销效率提升10倍，从此告别贴发票的烦恼！");
        templates.add(t1);

        Map<String, String> t2 = new HashMap<>();
        t2.put("type", "场景共鸣型");
        t2.put("title", "出差报销还在手忙脚乱？");
        t2.put("desc", "用钱酷报销，出差途中随手拍发票，回来一键生成报销单，同事都问我怎么做到的！");
        templates.add(t2);

        Map<String, String> t3 = new HashMap<>();
        t3.put("type", "数字冲击型");
        t3.put("title", "已帮用户节省100万+小时报销时间");
        t3.put("desc", "平均每笔报销从30分钟缩短到3分钟，钱酷报销，聪明人的报销工具。");
        templates.add(t3);

        Map<String, String> t4 = new HashMap<>();
        t4.put("type", "损失规避型");
        t4.put("title", "你的发票可能正在过期！");
        t4.put("desc", "钱酷报销智能提醒报销截止日期，别让到手的钱飞走，立即体验。");
        templates.add(t4);

        return templates;
    }
}
