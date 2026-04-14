package com.qiankubx.module.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileDTO {

    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;

    @Size(max = 500, message = "头像地址最多500个字符")
    private String avatarUrl;

    @Size(max = 100, message = "公司名最多100个字符")
    private String company;

    @Size(max = 100, message = "部门名最多100个字符")
    private String department;

    private BigDecimal defaultSubsidy;
}
