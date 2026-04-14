package com.qiankubx.module.expense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_expense")
public class Expense {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long categoryId;

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
