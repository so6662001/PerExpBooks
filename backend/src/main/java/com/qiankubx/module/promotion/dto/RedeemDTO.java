package com.qiankubx.module.promotion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RedeemDTO {

    /** 1=50积分换7天会员 2=100积分换¥10券 3=200积分换1月会员 4=500积分换年度会员 */
    @NotNull(message = "兑换类型不能为空")
    private Integer redeemType;
}
