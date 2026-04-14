package com.qiankubx.module.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_reimbursement")
public class Reimbursement {

    @TableId(type = IdType.ASSIGN_ID)
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
}
