package com.qiankubx.module.team.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeamVO {

    private Long id;

    private String name;

    private Long ownerId;

    private String ownerNickname;

    private String inviteCode;

    private Integer memberCount;

    private Integer maxMember;

    private Integer myRole;

    private LocalDateTime createdAt;
}
