package com.qiankubx.module.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_share_log")
public class ShareLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String shareType;

    private String contentType;

    private String shareScene;

    private Integer clickCount;

    private Integer registerCount;

    private LocalDateTime createdAt;
}
