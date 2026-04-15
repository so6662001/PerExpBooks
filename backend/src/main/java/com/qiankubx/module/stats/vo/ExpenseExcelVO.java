package com.qiankubx.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseExcelVO {

    private String expenseDate;

    private String categoryName;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private String invoiceNo;

    private String invoiceType;

    private String sellerName;

    private String description;

    private String reimburseStatusName;
}
