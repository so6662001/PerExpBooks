package com.qiankubx.module.team.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamMemberVO {

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private Integer role;

    private LocalDateTime joinedAt;
}
