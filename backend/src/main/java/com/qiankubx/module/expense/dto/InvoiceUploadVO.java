package com.qiankubx.module.expense.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceUploadVO {

    private String fileUrl;

    private String fileName;

    private String invoiceNo;

    private String invoiceCode;

    private LocalDate invoiceDate;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private String sellerName;

    private String buyerName;

    private String invoiceType;

    private Boolean parsedSuccess;

    private Boolean parseSuccess;

    private String parseMessage;
}
