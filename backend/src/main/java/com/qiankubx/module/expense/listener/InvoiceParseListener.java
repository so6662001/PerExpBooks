package com.qiankubx.module.expense.listener;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.config.RabbitMQConfig;
import com.qiankubx.module.expense.dto.InvoiceUploadVO;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import com.qiankubx.module.expense.service.InvoiceParseEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceParseListener {

    private final ExpenseMapper expenseMapper;
    private final InvoiceParseEngine invoiceParseEngine;
    private final OSS ossClient;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVOICE_PARSE)
    public void onMessage(Map<String, Object> message) {
        Long expenseId = message.get("expenseId") != null ? ((Number) message.get("expenseId")).longValue() : null;
        String fileUrl = (String) message.get("fileUrl");

        log.info("[MQ] 接收发票解析消息: expenseId={}, fileUrl={}", expenseId, fileUrl);

        if (expenseId == null || fileUrl == null) {
            log.warn("[MQ] 消息参数不完整, 跳过");
            return;
        }

        try {
            Expense expense = expenseMapper.selectById(expenseId);
            if (expense == null) {
                log.warn("[MQ] 费用记录不存在: expenseId={}", expenseId);
                return;
            }

            byte[] pdfBytes = downloadFromOss(fileUrl);
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.warn("[MQ] 文件下载失败: fileUrl={}", fileUrl);
                return;
            }

            InvoiceUploadVO parsed = invoiceParseEngine.parseFromPdf(pdfBytes);

            if (parsed.getParsedSuccess() != null && parsed.getParsedSuccess()) {
                LambdaUpdateWrapper<Expense> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Expense::getId, expenseId);
                if (parsed.getAmount() != null) wrapper.set(Expense::getAmount, parsed.getAmount());
                if (parsed.getTaxAmount() != null) wrapper.set(Expense::getTaxAmount, parsed.getTaxAmount());
                if (parsed.getInvoiceNo() != null) wrapper.set(Expense::getInvoiceNo, parsed.getInvoiceNo());
                if (parsed.getInvoiceCode() != null) wrapper.set(Expense::getInvoiceCode, parsed.getInvoiceCode());
                if (parsed.getInvoiceDate() != null) wrapper.set(Expense::getInvoiceDate, parsed.getInvoiceDate());
                if (parsed.getSellerName() != null) wrapper.set(Expense::getSellerName, parsed.getSellerName());
                if (parsed.getBuyerName() != null) wrapper.set(Expense::getBuyerName, parsed.getBuyerName());
                wrapper.set(Expense::getUpdatedAt, LocalDateTime.now());
                expenseMapper.update(null, wrapper);
                log.info("[MQ] 发票解析结果已更新: expenseId={}", expenseId);
            } else {
                log.info("[MQ] 发票解析未获取到有效数据: expenseId={}", expenseId);
            }
        } catch (Exception e) {
            log.error("[MQ] 发票解析处理失败: expenseId={}", expenseId, e);
        }
    }

    private byte[] downloadFromOss(String fileUrl) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            String bucketName = extractBucketName(fileUrl);
            OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
            return ossObject.getObjectContent().readAllBytes();
        } catch (Exception e) {
            log.error("OSS文件下载失败: {}", fileUrl, e);
            return null;
        }
    }

    private String extractObjectKey(String fileUrl) {
        int idx = fileUrl.indexOf("/", fileUrl.indexOf("//") + 2);
        return idx > 0 ? fileUrl.substring(idx + 1) : fileUrl;
    }

    private String extractBucketName(String fileUrl) {
        String host = fileUrl.replaceFirst("https?://", "");
        int dotIdx = host.indexOf(".");
        return dotIdx > 0 ? host.substring(0, dotIdx) : "default-bucket";
    }
}
