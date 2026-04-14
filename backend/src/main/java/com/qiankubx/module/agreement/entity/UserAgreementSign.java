package com.qiankubx.module.agreement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_agreement_sign")
public class UserAgreementSign {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long versionId;

    private String agreementType;

    private String versionCode;

    private String ipAddress;

    private String deviceInfo;

    private String userAgent;

    private String dataSign;

    private LocalDateTime signedAt;
}
