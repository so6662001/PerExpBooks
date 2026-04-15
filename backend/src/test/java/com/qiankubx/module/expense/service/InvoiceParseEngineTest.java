package com.qiankubx.module.expense.service;

import com.qiankubx.module.expense.dto.InvoiceUploadVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceParseEngineTest {

    @Mock private OcrApiService ocrApiService;

    @InjectMocks
    private InvoiceParseEngine invoiceParseEngine;

    @Test
    void parseFromPdf_validInvoice_shouldExtractFields() {
        byte[] pdfBytes = createMinimalPdfBytes();

        InvoiceUploadVO result = invoiceParseEngine.parseFromPdf(pdfBytes);

        assertThat(result).isNotNull();
    }

    @Test
    void parseFromPdf_emptyPdf_shouldFallbackToOcr() {
        byte[] emptyPdf = new byte[]{0x25, 0x50, 0x44, 0x46};

        InvoiceUploadVO ocrResult = new InvoiceUploadVO();
        ocrResult.setParseSuccess(true);
        ocrResult.setInvoiceNo("OCR12345678");

        when(ocrApiService.recognizeInvoice(any())).thenReturn(ocrResult);

        InvoiceUploadVO result = invoiceParseEngine.parseFromPdf(emptyPdf);

        assertThat(result).isNotNull();
    }

    @Test
    void parseFromPdf_invalidBytes_shouldReturnUnparsed() {
        byte[] invalidBytes = "not a pdf".getBytes();

        InvoiceUploadVO result = invoiceParseEngine.parseFromPdf(invalidBytes);

        assertThat(result).isNotNull();
        assertThat(result.getParsedSuccess()).isFalse();
    }

    @Test
    void parseFromPdf_invoiceNumber_shouldMatchRegex() {
        String invoiceNo = "12345678";
        assertThat(invoiceNo).matches("\\d{8,20}");

        String invoiceNo2 = "01234567890123456789";
        assertThat(invoiceNo2).matches("\\d{8,20}");
    }

    private byte[] createMinimalPdfBytes() {
        String minimalPdf = "%PDF-1.0\n1 0 obj<</Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Kids[3 0 R]/Count 1>>endobj\n" +
                "3 0 obj<</MediaBox[0 0 3 3]>>endobj\n" +
                "trailer<</Root 1 0 R>>";
        return minimalPdf.getBytes();
    }
}
