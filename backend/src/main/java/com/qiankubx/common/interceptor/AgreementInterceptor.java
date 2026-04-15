package com.qiankubx.common.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.mapper.AgreementVersionMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final AgreementVersionMapper agreementVersionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (userId == null) {
            return true;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return true;
        }

        AgreementVersion currentAgreement = getCurrentVersion("user_agreement");
        if (currentAgreement != null) {
            if (user.getAgreementVersionId() == null
                    || !user.getAgreementVersionId().equals(currentAgreement.getId())) {
                if ("major".equals(currentAgreement.getChangeLevel())) {
                    throw new BizException(ResultCode.AGREEMENT_REQUIRED);
                }
            }
        }

        AgreementVersion currentPrivacy = getCurrentVersion("privacy_policy");
        if (currentPrivacy != null) {
            if (user.getPrivacyVersionId() == null
                    || !user.getPrivacyVersionId().equals(currentPrivacy.getId())) {
                if ("major".equals(currentPrivacy.getChangeLevel())) {
                    throw new BizException(ResultCode.AGREEMENT_REQUIRED);
                }
            }
        }

        return true;
    }

    private AgreementVersion getCurrentVersion(String type) {
        return agreementVersionMapper.selectOne(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getType, type)
                        .eq(AgreementVersion::getStatus, 1)
                        .le(AgreementVersion::getEffectiveAt, LocalDateTime.now())
                        .orderByDesc(AgreementVersion::getEffectiveAt)
                        .last("LIMIT 1")
        );
    }
}
