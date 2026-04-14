package com.qiankubx.module.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinTeamDTO {

    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
