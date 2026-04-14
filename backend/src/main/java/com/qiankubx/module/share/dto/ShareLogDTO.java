package com.qiankubx.module.share.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShareLogDTO {

    @NotBlank(message = "分享类型不能为空")
    private String shareType;

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    private String shareScene;
}
