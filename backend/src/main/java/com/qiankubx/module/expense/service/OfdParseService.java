package com.qiankubx.module.expense.service;

import com.qiankubx.module.expense.dto.InvoiceUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OfdParseService {

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

    public InvoiceUploadVO parseFromOfd(byte[] ofdBytes) {
        InvoiceUploadVO result = new InvoiceUploadVO();
        result.setParsedSuccess(false);
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("invoice_", ".ofd");
            Files.write(tempFile, ofdBytes);

            try (OFDReader reader = new OFDReader(tempFile)) {
                ContentExtractor extractor = new ContentExtractor(reader);
                int pageCount = reader.getNumberOfPages();

                StringBuilder fullText = new StringBuilder();
                for (int i = 1; i <= pageCount; i++) {
                    List<String> lines = extractor.getPageContent(i);
                    if (lines != null) {
                        for (String line : lines) {
                            fullText.append(line).append("\n");
                        }
                    }
                }

                String text = fullText.toString();
                log.debug("OFD extracted text: {}", text);

                result.setInvoiceNo(extractMatch(INVOICE_NO_PATTERN, text));
                result.setInvoiceCode(extractMatch(INVOICE_CODE_PATTERN, text));

                String dateStr = extractMatch(INVOICE_DATE_PATTERN, text);
                if (dateStr != null) {
                    result.setInvoiceDate(parseChineseDate(dateStr));
                }

                String totalAmount = extractMatch(TOTAL_AMOUNT_PATTERN, text);
                if (totalAmount == null) {
                    totalAmount = extractMatch(TOTAL_AMOUNT_LOWER_PATTERN, text);
                }
                if (totalAmount != null) {
                    result.setAmount(parseBigDecimal(totalAmount));
                    result.setParsedSuccess(true);
                }

                String taxAmount = extractMatch(TAX_AMOUNT_PATTERN, text);
                if (taxAmount != null) {
                    result.setTaxAmount(parseBigDecimal(taxAmount));
                }

                result.setSellerName(extractMatch(SELLER_PATTERN, text));
                result.setBuyerName(extractMatch(BUYER_PATTERN, text));

                result.setParseSuccess(result.getParsedSuccess());
                result.setParseMessage(result.getParseSuccess() ? "OFD解析成功" : "OFD解析未能提取完整信息，请手动补充");
            }
        } catch (Exception e) {
            log.error("OFD解析失败", e);
            result.setParseSuccess(false);
            result.setParseMessage("OFD解析失败: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
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
