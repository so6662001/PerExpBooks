package com.qiankubx.module.agreement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.agreement.dto.*;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.entity.UserAgreementSign;
import com.qiankubx.module.agreement.mapper.AgreementVersionMapper;
import com.qiankubx.module.agreement.mapper.UserAgreementSignMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementService {

    private static final String TYPE_USER_AGREEMENT = "user_agreement";
    private static final String TYPE_PRIVACY_POLICY = "privacy_policy";

    private final AgreementVersionMapper agreementVersionMapper;
    private final UserAgreementSignMapper userAgreementSignMapper;
    private final UserMapper userMapper;
    private final DataSignService dataSignService;

    public AgreementCheckVO checkAgreement(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        AgreementCheckVO result = new AgreementCheckVO();
        List<AgreementCheckVO.AgreementItem> items = new ArrayList<>();
        boolean needConsent = false;
        boolean block = false;

        AgreementVersion currentAgreement = getCurrentVersion(TYPE_USER_AGREEMENT);
        if (currentAgreement != null) {
            if (user.getAgreementVersionId() == null
                    || !user.getAgreementVersionId().equals(currentAgreement.getId())) {
                needConsent = true;
                if ("major".equals(currentAgreement.getChangeLevel())) {
                    block = true;
                }
                AgreementCheckVO.AgreementItem item = new AgreementCheckVO.AgreementItem();
                item.setType(TYPE_USER_AGREEMENT);
                item.setVersionId(currentAgreement.getId());
                item.setVersionCode(currentAgreement.getVersionCode());
                item.setChangeSummary(currentAgreement.getChangeSummary());
                item.setChangeLevel(currentAgreement.getChangeLevel());
                items.add(item);
            }
        }

        AgreementVersion currentPrivacy = getCurrentVersion(TYPE_PRIVACY_POLICY);
        if (currentPrivacy != null) {
            if (user.getPrivacyVersionId() == null
                    || !user.getPrivacyVersionId().equals(currentPrivacy.getId())) {
                needConsent = true;
                if ("major".equals(currentPrivacy.getChangeLevel())) {
                    block = true;
                }
                AgreementCheckVO.AgreementItem item = new AgreementCheckVO.AgreementItem();
                item.setType(TYPE_PRIVACY_POLICY);
                item.setVersionId(currentPrivacy.getId());
                item.setVersionCode(currentPrivacy.getVersionCode());
                item.setChangeSummary(currentPrivacy.getChangeSummary());
                item.setChangeLevel(currentPrivacy.getChangeLevel());
                items.add(item);
            }
        }

        result.setNeedConsent(needConsent);
        result.setBlock(block);
        result.setAgreements(items);
        return result;
    }

    public AgreementVersion getCurrentVersion(String type) {
        return agreementVersionMapper.selectOne(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getType, type)
                        .eq(AgreementVersion::getStatus, 1)
                        .le(AgreementVersion::getEffectiveAt, LocalDateTime.now())
                        .orderByDesc(AgreementVersion::getEffectiveAt)
                        .last("LIMIT 1")
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void signAgreement(Long userId, AgreementSignDTO dto, HttpServletRequest request) {
        AgreementVersion version = agreementVersionMapper.selectById(dto.getVersionId());
        if (version == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "协议版本不存在");
        }

        if (!version.getType().equals(dto.getAgreementType())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "协议类型不匹配");
        }

        UserAgreementSign sign = new UserAgreementSign();
        sign.setUserId(userId);
        sign.setVersionId(version.getId());
        sign.setAgreementType(version.getType());
        sign.setVersionCode(version.getVersionCode());
        sign.setIpAddress(getClientIp(request));
        sign.setDeviceInfo(request.getHeader("X-Device-Info"));
        sign.setUserAgent(request.getHeader("User-Agent"));
        sign.setSignedAt(LocalDateTime.now());

        String payload = userId + "|" + version.getId() + "|" + version.getType()
                + "|" + version.getVersionCode() + "|" + sign.getSignedAt();
        sign.setDataSign(dataSignService.sign(payload));

        userAgreementSignMapper.insert(sign);

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        if (TYPE_USER_AGREEMENT.equals(version.getType())) {
            updateWrapper.set(User::getAgreementVersionId, version.getId());
            updateWrapper.set(User::getAgreementSignedAt, sign.getSignedAt());
        } else if (TYPE_PRIVACY_POLICY.equals(version.getType())) {
            updateWrapper.set(User::getPrivacyVersionId, version.getId());
            updateWrapper.set(User::getPrivacySignedAt, sign.getSignedAt());
        }
        updateWrapper.set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, updateWrapper);
    }

    public List<AgreementVersionVO> getHistory(String type) {
        List<AgreementVersion> versions = agreementVersionMapper.selectList(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getType, type)
                        .eq(AgreementVersion::getStatus, 1)
                        .orderByDesc(AgreementVersion::getEffectiveAt)
        );
        return versions.stream().map(this::toVersionVO).collect(Collectors.toList());
    }

    public AgreementVersionVO getVersion(Long versionId) {
        AgreementVersion version = agreementVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "协议版本不存在");
        }
        return toVersionVO(version);
    }

    public List<UserAgreementSignVO> getMySigns(Long userId) {
        List<UserAgreementSign> signs = userAgreementSignMapper.selectList(
                new LambdaQueryWrapper<UserAgreementSign>()
                        .eq(UserAgreementSign::getUserId, userId)
                        .orderByDesc(UserAgreementSign::getSignedAt)
        );
        return signs.stream().map(this::toSignVO).collect(Collectors.toList());
    }

    private AgreementVersionVO toVersionVO(AgreementVersion version) {
        AgreementVersionVO vo = new AgreementVersionVO();
        vo.setId(version.getId());
        vo.setType(version.getType());
        vo.setVersionCode(version.getVersionCode());
        vo.setTitle(version.getTitle());
        vo.setContent(version.getContent());
        vo.setChangeSummary(version.getChangeSummary());
        vo.setChangeLevel(version.getChangeLevel());
        vo.setEffectiveAt(version.getEffectiveAt());
        vo.setCreatedAt(version.getCreatedAt());
        return vo;
    }

    private UserAgreementSignVO toSignVO(UserAgreementSign sign) {
        UserAgreementSignVO vo = new UserAgreementSignVO();
        vo.setId(sign.getId());
        vo.setAgreementType(sign.getAgreementType());
        vo.setVersionCode(sign.getVersionCode());
        vo.setIpAddress(sign.getIpAddress());
        vo.setSignedAt(sign.getSignedAt());
        return vo;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
