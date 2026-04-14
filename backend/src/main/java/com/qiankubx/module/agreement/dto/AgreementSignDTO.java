package com.qiankubx.module.agreement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgreementSignDTO {

    @NotBlank(message = "协议类型不能为空")
    private String agreementType;

    @NotNull(message = "协议版本ID不能为空")
    private Long versionId;
}
