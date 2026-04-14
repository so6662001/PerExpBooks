package com.qiankubx.module.expense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_business_trip")
public class BusinessTrip {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String title;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer days;

    private BigDecimal subsidyPerDay;

    private BigDecimal subsidyTotal;

    private String remark;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
