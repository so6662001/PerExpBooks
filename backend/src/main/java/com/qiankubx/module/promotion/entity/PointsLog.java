package com.qiankubx.module.promotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_points_log")
public class PointsLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Integer points;

    private Integer balanceAfter;

    /** invite_register / invite_paid / redeem / sign_in 等 */
    private String action;

    private Long refId;

    private String remark;

    private String chainHash;

    private String dataSign;

    private LocalDateTime createdAt;
}
