package com.qiankubx.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberStatusVO {

    private Integer memberType;

    private Integer memberStatus;

    private LocalDateTime memberExpireTime;

    private LocalDateTime trialEndTime;

    private Boolean isTrial;

    private Boolean isExpired;
}
