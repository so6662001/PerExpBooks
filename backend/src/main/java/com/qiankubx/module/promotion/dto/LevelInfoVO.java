package com.qiankubx.module.promotion.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LevelInfoVO {

    private Integer level;

    private String levelName;

    private BigDecimal level1Rate;

    private BigDecimal level2Rate;

    private Integer inviteCount;

    private Integer paidInviteCount;

    private Integer nextLevelInviteRequired;

    private String nextLevelName;

    private BigDecimal totalCommission;

    private Integer points;

    private Integer totalPoints;
}
