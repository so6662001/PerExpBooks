package com.qiankubx.module.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTeamDTO {

    @NotBlank(message = "团队名称不能为空")
    private String name;
}
