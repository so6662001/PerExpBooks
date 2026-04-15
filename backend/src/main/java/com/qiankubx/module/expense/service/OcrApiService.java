package com.qiankubx.module.expense.service;

import com.qiankubx.module.expense.dto.InvoiceUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class OcrApiService {

    @Value("${qianku.ocr.baidu.api-key:}")
    private String apiKey;

    @Value("${qianku.ocr.baidu.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public InvoiceUploadVO recognizeInvoice(byte[] imageBytes) {
        InvoiceUploadVO result = new InvoiceUploadVO();

        if (apiKey == null || apiKey.isBlank()) {
            result.setParseSuccess(false);
            result.setParsedSuccess(false);
            result.setParseMessage("百度OCR未配置，请手动填写发票信息");
            return result;
        }

        try {
            String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials"
                    + "&client_id=" + apiKey + "&client_secret=" + secretKey;
            Map<String, Object> tokenResp = restTemplate.postForObject(tokenUrl, null, Map.class);
            String accessToken = (String) tokenResp.get("access_token");

            String ocrUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/vat_invoice?access_token=" + accessToken;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("image", base64Image);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            Map<String, Object> ocrResult = restTemplate.postForObject(ocrUrl, request, Map.class);

            if (ocrResult != null && ocrResult.containsKey("words_result")) {
                Map<String, Object> words = (Map<String, Object>) ocrResult.get("words_result");
                result.setInvoiceNo(getOcrField(words, "InvoiceNum"));
                result.setInvoiceCode(getOcrField(words, "InvoiceCode"));
                result.setAmount(parseAmount(getOcrField(words, "AmountInFiguers")));
                result.setTaxAmount(parseAmount(getOcrField(words, "TotalTax")));
                result.setSellerName(getOcrField(words, "SellerName"));
                result.setBuyerName(getOcrField(words, "PurchaserName"));
                result.setParseSuccess(true);
                result.setParsedSuccess(true);
                result.setParseMessage("OCR识别成功");
            } else {
                result.setParseSuccess(false);
                result.setParsedSuccess(false);
                result.setParseMessage("OCR识别未返回有效结果");
            }
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            result.setParseSuccess(false);
            result.setParsedSuccess(false);
            result.setParseMessage("OCR识别失败: " + e.getMessage());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String getOcrField(Map<String, Object> words, String key) {
        if (words.containsKey(key)) {
            Object field = words.get(key);
            if (field instanceof Map) {
                return (String) ((Map<String, Object>) field).get("word");
            } else if (field instanceof String) {
                return (String) field;
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) return null;
        try {
            return new BigDecimal(amountStr.replaceAll("[^\\d.]", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
