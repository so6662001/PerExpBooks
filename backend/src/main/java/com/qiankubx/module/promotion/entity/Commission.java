package com.qiankubx.module.promotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_commission")
public class Commission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long orderId;

    private Long inviteeId;

    /** 1一级返佣 2二级返佣 */
    private Integer level;

    /** 1一级首购 2一级续费 3二级首购 4二级续费 */
    private Integer commissionType;

    private BigDecimal orderAmount;

    private BigDecimal commissionRate;

    private BigDecimal commissionAmount;

    /** 0冻结 1已结算 2已撤销 */
    private Integer status;

    private LocalDateTime settleTime;

    private String dataSign;

    private LocalDateTime createdAt;
}
