package com.qiankubx.module.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_team_member")
public class TeamMember {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long teamId;

    private Long userId;

    /** 1管理员 2普通成员 */
    private Integer role;

    /** 0正常 1已移除 */
    private Integer status;

    private LocalDateTime joinedAt;

    private LocalDateTime createdAt;
}
