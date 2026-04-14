package com.qiankubx.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginDTO {

    @NotBlank(message = "微信授权code不能为空")
    private String code;

    private String inviteCode;
}
