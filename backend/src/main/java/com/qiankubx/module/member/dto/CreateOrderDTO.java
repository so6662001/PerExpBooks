package com.qiankubx.module.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderDTO {

    @NotNull(message = "套餐类型不能为空")
    private Integer planType;

    @NotNull(message = "支付方式不能为空")
    private Integer payType;

    private Long couponId;

    /** 团队版成员数 */
    private Integer teamMemberCount;
}
