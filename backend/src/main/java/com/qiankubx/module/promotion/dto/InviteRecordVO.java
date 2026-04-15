package com.qiankubx.module.promotion.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InviteRecordVO {

    private Long inviteeId;

    private String inviteeNickname;

    private String inviteeAvatarUrl;

    private Integer level;

    private Integer inviteeStatus;

    private LocalDateTime createdAt;
}
