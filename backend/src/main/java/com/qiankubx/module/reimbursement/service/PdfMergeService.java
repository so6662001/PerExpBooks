package com.qiankubx.module.reimbursement.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.qiankubx.common.config.OssConfig;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.reimbursement.entity.Reimbursement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfMergeService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    public String mergePdfs(Reimbursement reimbursement, List<Expense> expenses) {
        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream();
            merger.setDestinationStream(mergedOutput);

            if (reimbursement.getPdfUrl() != null && !reimbursement.getPdfUrl().isBlank()) {
                String coverKey = extractOssKey(reimbursement.getPdfUrl());
                byte[] coverBytes = downloadBytes(coverKey);
                if (coverBytes != null) {
                    merger.addSource(new RandomAccessReadBuffer(coverBytes));
                }
            }

            for (Expense expense : expenses) {
                if (expense.getFileUrl() != null && !expense.getFileUrl().isBlank()
                        && expense.getFileUrl().toLowerCase().endsWith(".pdf")) {
                    String expenseKey = extractOssKey(expense.getFileUrl());
                    try {
                        byte[] bytes = downloadBytes(expenseKey);
                        if (bytes != null) {
                            merger.addSource(new RandomAccessReadBuffer(bytes));
                        }
                    } catch (Exception e) {
                        log.warn("下载费用PDF失败, expenseId={}, url={}", expense.getId(), expense.getFileUrl(), e);
                    }
                }
            }

            merger.mergeDocuments(null);

            String objectKey = ossConfig.getDirs().getMerged()
                    + reimbursement.getReimburseNo() + "_merged.pdf";
            byte[] mergedBytes = mergedOutput.toByteArray();
            ossClient.putObject(ossConfig.getBucketName(), objectKey,
                    new ByteArrayInputStream(mergedBytes));

            String url = buildOssUrl(objectKey);
            log.info("合并PDF已上传: {}", url);
            return url;
        } catch (Exception e) {
            log.error("合并PDF失败", e);
            throw new RuntimeException("合并PDF失败: " + e.getMessage(), e);
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

    String extractOssKey(String url) {
        if (url == null) {
            return null;
        }
        if (ossConfig.getCdnDomain() != null && !ossConfig.getCdnDomain().isBlank()) {
            String domain = ossConfig.getCdnDomain();
            if (!domain.startsWith("http")) {
                domain = "https://" + domain;
            }
            if (url.startsWith(domain)) {
                String key = url.substring(domain.length());
                return key.startsWith("/") ? key.substring(1) : key;
            }
        }
        int idx = url.indexOf(".com/");
        if (idx > 0) {
            return url.substring(idx + 5);
        }
        int protoEnd = url.indexOf("://");
        if (protoEnd > 0) {
            int slashIdx = url.indexOf('/', protoEnd + 3);
            if (slashIdx > 0) {
                return url.substring(slashIdx + 1);
            }
        }
        return url;
    }

    private String buildOssUrl(String objectKey) {
        if (ossConfig.getCdnDomain() != null && !ossConfig.getCdnDomain().isBlank()) {
            String domain = ossConfig.getCdnDomain();
            if (!domain.startsWith("http")) {
                domain = "https://" + domain;
            }
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            return domain + "/" + objectKey;
        }
        return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectKey;
    }
}
