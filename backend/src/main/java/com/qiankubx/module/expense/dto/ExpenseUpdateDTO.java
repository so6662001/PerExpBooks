package com.qiankubx.module.expense.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseUpdateDTO {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long tripId;

    @NotNull(message = "费用类型不能为空")
    private Integer type;

    @NotNull(message = "金额不能为空")
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

    @NotNull(message = "费用日期不能为空")
    private LocalDate expenseDate;
}
