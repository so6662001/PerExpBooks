package com.qiankubx.module.promotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_withdrawal")
public class Withdrawal {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private BigDecimal amount;

    /** 1微信 2支付宝 3银行卡 */
    private Integer withdrawType;

    private String accountInfo;

    /** 0处理中 1已完成 2已拒绝 */
    private Integer status;

    private String rejectReason;

    private LocalDateTime processedAt;

    private String dataSign;

    private LocalDateTime createdAt;
}
