package com.qiankubx.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_user_coupon")
public class UserCoupon {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long templateId;

    private String name;

    /** 1满减 2折扣 3固定金额 */
    private Integer type;

    private BigDecimal discountValue;

    private BigDecimal minAmount;

    /** 适用套餐类型: 0全部 1月度 2年度 3团队 */
    private Integer applicablePlanType;

    /** 0未使用 1已使用 2已过期 */
    private Integer useStatus;

    private Long orderId;

    private LocalDateTime usedAt;

    private LocalDateTime expireAt;

    private String dataSign;

    private LocalDateTime createdAt;
}
