package com.qiankubx.module.agreement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_agreement_version")
public class AgreementVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 协议类型: user_agreement / privacy_policy */
    private String type;

    private String versionCode;

    private String title;

    private String content;

    private String changeSummary;

    /** 变更级别: minor / major */
    private String changeLevel;

    /** 状态: 0=草稿, 1=生效 */
    private Integer status;

    private LocalDateTime effectiveAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
