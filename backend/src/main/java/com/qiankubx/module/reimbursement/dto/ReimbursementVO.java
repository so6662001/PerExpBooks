package com.qiankubx.module.reimbursement.dto;

import com.qiankubx.module.expense.dto.ExpenseVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReimbursementVO {

    private Long id;

    private Long userId;

    private String reimburseNo;

    private String title;

    private BigDecimal totalAmount;

    private Integer invoiceCount;

    private Integer itemCount;

    private String remark;

    private String pdfUrl;

    private String mergedPdfUrl;

    private String zipUrl;

    private Long tripId;

    private Integer reimburseStatus;

    private Integer exportCount;

    private LocalDateTime exportedAt;

    private Integer emailSent;

    private String emailAddress;

    private LocalDateTime emailSentAt;

    private LocalDateTime receivedAt;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ExpenseVO> expenses;
}
