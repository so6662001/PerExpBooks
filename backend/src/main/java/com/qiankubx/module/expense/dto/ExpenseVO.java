package com.qiankubx.module.expense.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpenseVO {

    private Long id;

    private Long userId;

    private Long categoryId;

    private String categoryName;

    private Long tripId;

    private Integer type;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private String invoiceNo;

    private String invoiceCode;

    private LocalDate invoiceDate;

    private String invoiceType;

    private String sellerName;

    private String buyerName;

    private String fileUrl;

    private String fileName;

    private String description;

    private LocalDate expenseDate;

    private Integer reimburseStatus;

    private Long reimbursementId;

    private String dataSign;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
