package com.qiankubx.module.promotion.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointsLogVO {

    private Long id;

    private Integer points;

    private Integer balanceAfter;

    private String action;

    private String remark;

    private LocalDateTime createdAt;
}
