package com.qiankubx.module.promotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_promoter_level")
public class PromoterLevel {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 1新手 2银牌 3金牌 4钻石 */
    private Integer level;

    private String levelName;

    private BigDecimal level1Rate;

    private BigDecimal level2Rate;

    private Integer inviteCount;

    private Integer paidInviteCount;

    private BigDecimal totalCommission;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal withdrawnAmount;

    private Integer points;

    private Integer totalPoints;

    private String dataSign;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
