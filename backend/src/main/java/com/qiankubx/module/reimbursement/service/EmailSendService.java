package com.qiankubx.module.reimbursement.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.qiankubx.common.config.OssConfig;
import com.qiankubx.module.reimbursement.entity.Reimbursement;
import com.qiankubx.module.user.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendService {

    private final JavaMailSender mailSender;
    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final PdfMergeService pdfMergeService;

    public void sendReimbursementEmail(Reimbursement reimbursement, User user,
                                       String toEmail, int attachType, String customMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String nickname = user.getNickname() != null ? user.getNickname() : "用户";
            String subject = "费用报销单_" + reimbursement.getReimburseNo() + "_" + nickname;
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String htmlBody = buildHtmlBody(reimbursement, user, customMessage);
            helper.setText(htmlBody, true);

            addAttachment(helper, reimbursement, attachType);

            mailSender.send(mimeMessage);
            log.info("报销邮件已发送: to={}, reimburseNo={}", toEmail, reimbursement.getReimburseNo());
        } catch (Exception e) {
            log.error("发送报销邮件失败", e);
            throw new RuntimeException("发送邮件失败: " + e.getMessage(), e);
        }
    }

    private String buildHtmlBody(Reimbursement reimbursement, User user, String customMessage) {
        String nickname = user.getNickname() != null ? user.getNickname() : "用户";
        String company = user.getCompany() != null ? user.getCompany() : "-";
        String department = user.getDepartment() != null ? user.getDepartment() : "-";
        String createdDate = reimbursement.getCreatedAt() != null
                ? reimbursement.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "-";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        sb.append("<h2 style='color: #333;'>费用报销单</h2>");

        if (customMessage != null && !customMessage.isBlank()) {
            sb.append("<p style='color: #666; margin-bottom: 20px;'>").append(customMessage).append("</p>");
        }

        sb.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        appendRow(sb, "报销单号", reimbursement.getReimburseNo());
        appendRow(sb, "报销标题", reimbursement.getTitle());
        appendRow(sb, "报销人", nickname);
        appendRow(sb, "公司", company);
        appendRow(sb, "部门", department);
        appendRow(sb, "报销金额", "¥ " + reimbursement.getTotalAmount().toPlainString());
        appendRow(sb, "发票数量", String.valueOf(reimbursement.getInvoiceCount()));
        appendRow(sb, "费用项数", String.valueOf(reimbursement.getItemCount()));
        appendRow(sb, "申请日期", createdDate);
        if (reimbursement.getRemark() != null && !reimbursement.getRemark().isBlank()) {
            appendRow(sb, "备注", reimbursement.getRemark());
        }
        sb.append("</table>");

        sb.append("<p style='color: #999; font-size: 12px;'>此邮件由钱酷报销系统自动发送，请勿直接回复。</p>");
        sb.append("</div>");

        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String label, String value) {
        sb.append("<tr>");
        sb.append("<td style='padding: 8px 12px; border: 1px solid #eee; background: #f9f9f9; font-weight: bold; width: 120px;'>");
        sb.append(label);
        sb.append("</td>");
        sb.append("<td style='padding: 8px 12px; border: 1px solid #eee;'>");
        sb.append(value != null ? value : "-");
        sb.append("</td>");
        sb.append("</tr>");
    }

    private void addAttachment(MimeMessageHelper helper, Reimbursement reimbursement, int attachType) throws Exception {
        String fileUrl;
        String fileName;

        if (attachType == 2) {
            fileUrl = reimbursement.getZipUrl();
            fileName = reimbursement.getReimburseNo() + ".zip";
        } else {
            fileUrl = reimbursement.getMergedPdfUrl();
            fileName = reimbursement.getReimburseNo() + "_merged.pdf";
        }

        if (fileUrl == null || fileUrl.isBlank()) {
            log.warn("附件URL为空, reimburseNo={}", reimbursement.getReimburseNo());
            return;
        }

        String objectKey = pdfMergeService.extractOssKey(fileUrl);
        byte[] data = downloadBytes(objectKey);
        if (data != null) {
            helper.addAttachment(fileName, new ByteArrayResource(data));
        }
    }

    private byte[] downloadBytes(String objectKey) {
        try {
            OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), objectKey);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream is = ossObject.getObjectContent()) {
                is.transferTo(buffer);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            log.warn("从OSS下载文件失败: {}", objectKey, e);
            return null;
        }
    }
}
