package com.qiankubx.module.promotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_invitation")
public class Invitation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long inviterId;

    private Long inviteeId;

    /** 1一级 2二级 */
    private Integer level;

    /** 0已注册 1已付费 */
    private Integer inviteeStatus;

    private LocalDateTime createdAt;
}
