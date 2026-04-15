package com.qiankubx.module.reimbursement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailSendDTO {

    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 1: 合并PDF  2: ZIP
     */
    private Integer attachType = 1;

    private String message;
}
