package com.qiankubx.module.expense.service;

import com.qiankubx.module.expense.dto.InvoiceUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class InvoiceParseEngine {

    private final OcrApiService ocrApiService;

    public InvoiceParseEngine(OcrApiService ocrApiService) {
        this.ocrApiService = ocrApiService;
    }

    private static final Pattern INVOICE_NO_PATTERN =
            Pattern.compile("发票号码[：:]\\s*(\\d{8,20})");
    private static final Pattern INVOICE_CODE_PATTERN =
            Pattern.compile("发票代码[：:]\\s*(\\d{10,12})");
    private static final Pattern INVOICE_DATE_PATTERN =
            Pattern.compile("开票日期[：:]\\s*(\\d{4}年\\d{1,2}月\\d{1,2}日)");
    private static final Pattern TOTAL_AMOUNT_PATTERN =
            Pattern.compile("价税合计.*?[¥￥]\\s*([\\d,.]+)");
    private static final Pattern TOTAL_AMOUNT_LOWER_PATTERN =
            Pattern.compile("小写.*?[¥￥]\\s*([\\d,.]+)");
    private static final Pattern TAX_AMOUNT_PATTERN =
            Pattern.compile("税额.*?[¥￥]\\s*([\\d,.]+)");
    private static final Pattern SELLER_PATTERN =
            Pattern.compile("销[售方].*?名称[：:]\\s*(.+?)(?:\\s|$)");
    private static final Pattern BUYER_PATTERN =
            Pattern.compile("购[买方].*?名称[：:]\\s*(.+?)(?:\\s|$)");

    public InvoiceUploadVO parseFromPdf(byte[] pdfBytes) {
        InvoiceUploadVO vo = parseWithPdfBox(pdfBytes);
        if ((vo.getParsedSuccess() == null || !vo.getParsedSuccess()) || vo.getAmount() == null) {
            log.info("PDFBox解析结果不完整，降级到OCR识别");
            try {
                InvoiceUploadVO ocrResult = ocrApiService.recognizeInvoice(pdfBytes);
                if (ocrResult.getParseSuccess() != null && ocrResult.getParseSuccess()) {
                    return ocrResult;
                }
            } catch (Exception e) {
                log.warn("OCR降级识别失败", e);
            }
        }
        return vo;
    }

    private InvoiceUploadVO parseWithPdfBox(byte[] pdfBytes) {
        InvoiceUploadVO vo = new InvoiceUploadVO();
        vo.setParsedSuccess(false);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.debug("PDF extracted text: {}", text);

            vo.setInvoiceNo(extractMatch(INVOICE_NO_PATTERN, text));
            vo.setInvoiceCode(extractMatch(INVOICE_CODE_PATTERN, text));

            String dateStr = extractMatch(INVOICE_DATE_PATTERN, text);
            if (dateStr != null) {
                vo.setInvoiceDate(parseChineseDate(dateStr));
            }

            String totalAmount = extractMatch(TOTAL_AMOUNT_PATTERN, text);
            if (totalAmount == null) {
                totalAmount = extractMatch(TOTAL_AMOUNT_LOWER_PATTERN, text);
            }
            if (totalAmount != null) {
                vo.setAmount(parseBigDecimal(totalAmount));
                vo.setParsedSuccess(true);
            }

            String taxAmount = extractMatch(TAX_AMOUNT_PATTERN, text);
            if (taxAmount != null) {
                vo.setTaxAmount(parseBigDecimal(taxAmount));
            }

            vo.setSellerName(extractMatch(SELLER_PATTERN, text));
            vo.setBuyerName(extractMatch(BUYER_PATTERN, text));

        } catch (Exception e) {
            log.error("PDF解析失败", e);
        }

        return vo;
    }

    private String extractMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private LocalDate parseChineseDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            String cleaned = value.replace(",", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            log.warn("金额解析失败: {}", value);
            return null;
        }
    }
}
