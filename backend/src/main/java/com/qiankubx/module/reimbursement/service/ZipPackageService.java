package com.qiankubx.module.reimbursement.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.qiankubx.common.config.OssConfig;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.reimbursement.entity.Reimbursement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    public String generateZip(Reimbursement reimbursement, List<Expense> expenses, String mergedPdfUrl) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            addCoverPdf(zos, reimbursement);
            addExpenseFiles(zos, expenses);
            addExcelManifest(zos, expenses, reimbursement);
            addMergedPdf(zos, mergedPdfUrl);

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

    public String generateZip(Reimbursement reimbursement, List<Expense> expenses) {
        return generateZip(reimbursement, expenses, null);
    }

    private void addMergedPdf(ZipOutputStream zos, String mergedPdfUrl) throws Exception {
        if (mergedPdfUrl == null || mergedPdfUrl.isBlank()) {
            return;
        }
        String key = pdfMergeService.extractOssKey(mergedPdfUrl);
        byte[] data = downloadBytes(key);
        if (data != null) {
            zos.putNextEntry(new ZipEntry("合并版_全部文件.pdf"));
            zos.write(data);
            zos.closeEntry();
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
        byte[] excelBytes = generateExcelBytes(reimbursement, expenses);
        zos.putNextEntry(new ZipEntry("发票清单_" + reimbursement.getReimburseNo() + ".xlsx"));
        zos.write(excelBytes);
        zos.closeEntry();
    }

    private byte[] generateExcelBytes(Reimbursement reimbursement, List<Expense> expenses) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("发票清单");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            XSSFRow headerRow = sheet.createRow(0);
            String[] headers = {"序号", "类别", "发票号码", "金额(元)", "日期", "销方", "备注", "有原件"};
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(e.getInvoiceType() != null ? e.getInvoiceType() : "其他");
                row.createCell(2).setCellValue(e.getInvoiceNo() != null ? e.getInvoiceNo() : "-");
                row.createCell(3).setCellValue(e.getAmount() != null ? e.getAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(e.getExpenseDate() != null ? e.getExpenseDate().toString() : "");
                row.createCell(5).setCellValue(e.getSellerName() != null ? e.getSellerName() : "");
                row.createCell(6).setCellValue(e.getDescription() != null ? e.getDescription() : "");
                row.createCell(7).setCellValue(e.getFileUrl() != null && !e.getFileUrl().isBlank() ? "是" : "否");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
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
}
