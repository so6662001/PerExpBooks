package com.qiankubx.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String openid;

    private String unionid;

    private String phone;

    private String nickname;

    private String avatarUrl;

    private String company;

    private String department;

    private String inviteCode;

    private Long inviterId;

    private Long rootInviterId;

    private Integer memberType;

    private Integer memberStatus;

    private LocalDateTime memberExpireTime;

    private LocalDateTime trialEndTime;

    private Long teamId;

    private Integer monthlyInvoiceUsed;

    private Integer monthlyReimburseUsed;

    private BigDecimal defaultSubsidy;

    private Long agreementVersionId;

    private Long privacyVersionId;

    private LocalDateTime agreementSignedAt;

    private LocalDateTime privacySignedAt;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
