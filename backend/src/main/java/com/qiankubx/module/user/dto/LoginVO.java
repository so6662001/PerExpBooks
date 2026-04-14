package com.qiankubx.module.user.dto;

import lombok.Data;

@Data
public class LoginVO {

    private String token;

    private Boolean isNew;

    private UserVO user;
}
