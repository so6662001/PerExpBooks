package com.qiankubx.module.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_member_order")
public class MemberOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String orderNo;

    /** 1月度 2年度 3团队 */
    private Integer planType;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private Long couponId;

    /** 1微信 2支付宝 */
    private Integer payType;

    /** 0待支付 1已支付 2已关闭 */
    private Integer payStatus;

    private LocalDateTime payTime;

    private String tradeNo;

    private LocalDateTime memberStart;

    private LocalDateTime memberEnd;

    private Long teamId;

    /** 团队版成员数 */
    private Integer teamMemberCount;

    /** 0否 1是 */
    private Integer isRenewal;

    /** 0无 1退款中 2已退款 */
    private Integer refundStatus;

    private LocalDateTime refundTime;

    private String dataSign;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
