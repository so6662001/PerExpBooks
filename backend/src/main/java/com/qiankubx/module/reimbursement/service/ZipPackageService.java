package com.qiankubx.module.reimbursement.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.qiankubx.common.config.OssConfig;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.reimbursement.entity.Reimbursement;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipPackageService {

    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final PdfMergeService pdfMergeService;

    public String generateZip(Reimbursement reimbursement, List<Expense> expenses) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            addCoverPdf(zos, reimbursement);
            addExpenseFiles(zos, expenses);
            addExcelManifest(zos, expenses, reimbursement);

            zos.close();

            String objectKey = ossConfig.getDirs().getZip()
                    + reimbursement.getReimburseNo() + ".zip";
            ossClient.putObject(ossConfig.getBucketName(), objectKey,
                    new ByteArrayInputStream(baos.toByteArray()));

            String url = buildOssUrl(objectKey);
            log.info("ZIP打包已上传: {}", url);
            return url;
        } catch (Exception e) {
            log.error("生成ZIP失败", e);
            throw new RuntimeException("生成ZIP失败: " + e.getMessage(), e);
        }
    }

    private void addCoverPdf(ZipOutputStream zos, Reimbursement reimbursement) throws Exception {
        if (reimbursement.getPdfUrl() == null || reimbursement.getPdfUrl().isBlank()) {
            return;
        }
        String key = pdfMergeService.extractOssKey(reimbursement.getPdfUrl());
        byte[] data = downloadBytes(key);
        if (data != null) {
            zos.putNextEntry(new ZipEntry("报销单_" + reimbursement.getReimburseNo() + ".pdf"));
            zos.write(data);
            zos.closeEntry();
        }
    }

    private void addExpenseFiles(ZipOutputStream zos, List<Expense> expenses) throws Exception {
        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            if (expense.getFileUrl() == null || expense.getFileUrl().isBlank()) {
                continue;
            }
            String key = pdfMergeService.extractOssKey(expense.getFileUrl());
            byte[] data = downloadBytes(key);
            if (data == null) {
                continue;
            }
            String ext = getFileExtension(expense.getFileUrl());
            String category = expense.getInvoiceType() != null ? expense.getInvoiceType() : "其他";
            String amountStr = expense.getAmount() != null ? expense.getAmount().toPlainString() : "0";
            String fileName = String.format("发票/%02d_%s_%s%s", i + 1, category, amountStr, ext);

            zos.putNextEntry(new ZipEntry(fileName));
            zos.write(data);
            zos.closeEntry();
        }
    }

    private void addExcelManifest(ZipOutputStream zos, List<Expense> expenses,
                                  Reimbursement reimbursement) throws Exception {
        List<InvoiceExcelRow> rows = new ArrayList<>();
        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            InvoiceExcelRow row = new InvoiceExcelRow();
            row.setIndex(i + 1);
            row.setCategoryName(expense.getInvoiceType());
            row.setInvoiceNo(expense.getInvoiceNo());
            row.setAmount(expense.getAmount() != null ? expense.getAmount().toPlainString() : "0");
            row.setExpenseDate(expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : "");
            row.setSeller(expense.getSellerName());
            row.setRemark(expense.getDescription());
            row.setHasFile(expense.getFileUrl() != null && !expense.getFileUrl().isBlank() ? "是" : "否");
            rows.add(row);
        }

        ByteArrayOutputStream excelBaos = new ByteArrayOutputStream();
        EasyExcel.write(excelBaos, InvoiceExcelRow.class)
                .sheet("发票清单")
                .doWrite(rows);

        zos.putNextEntry(new ZipEntry("发票清单_" + reimbursement.getReimburseNo() + ".xlsx"));
        zos.write(excelBaos.toByteArray());
        zos.closeEntry();
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

    private String getFileExtension(String url) {
        if (url == null) return "";
        String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx > 0) {
            return path.substring(dotIdx);
        }
        return ".pdf";
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

    @Data
    public static class InvoiceExcelRow {
        @ExcelProperty("序号")
        private Integer index;
        @ExcelProperty("类别")
        private String categoryName;
        @ExcelProperty("发票号码")
        private String invoiceNo;
        @ExcelProperty("金额(元)")
        private String amount;
        @ExcelProperty("日期")
        private String expenseDate;
        @ExcelProperty("销方")
        private String seller;
        @ExcelProperty("备注")
        private String remark;
        @ExcelProperty("有原件")
        private String hasFile;
    }
}
