package com.qiankubx.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    /** 1满减 2折扣 3固定金额 */
    private Integer type;

    private BigDecimal discountValue;

    private BigDecimal minAmount;

    /** 适用套餐类型: 0全部 1月度 2年度 3团队 */
    private Integer applicablePlanType;

    private Integer validDays;

    private Integer totalCount;

    private Integer issuedCount;

    /** 1新人券 2邀请券 3活动券 */
    private Integer issueType;

    /** 0下架 1上架 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
