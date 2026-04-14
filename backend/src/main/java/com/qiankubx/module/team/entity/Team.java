package com.qiankubx.module.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_team")
public class Team {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Long ownerId;

    private String inviteCode;

    private Integer memberCount;

    private Integer maxMember;

    /** 0正常 1已解散 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
