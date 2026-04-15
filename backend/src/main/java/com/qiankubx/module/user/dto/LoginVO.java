package com.qiankubx.module.user.dto;

import com.qiankubx.module.agreement.dto.AgreementCheckVO;
import lombok.Data;

@Data
public class LoginVO {

    private String token;

    private Boolean isNew;

    private UserVO user;

    private MemberStatusVO memberStatus;

    private AgreementCheckVO agreementCheck;
}
