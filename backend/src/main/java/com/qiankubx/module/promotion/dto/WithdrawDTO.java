package com.qiankubx.module.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawDTO {

    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "50", message = "最低提现金额为50元")
    private BigDecimal amount;

    @NotNull(message = "提现方式不能为空")
    private Integer withdrawType;
}
