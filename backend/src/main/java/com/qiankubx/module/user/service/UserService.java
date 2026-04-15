package com.qiankubx.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.common.util.JwtUtil;
import com.qiankubx.module.agreement.dto.AgreementCheckVO;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.entity.UserAgreementSign;
import com.qiankubx.module.agreement.mapper.AgreementVersionMapper;
import com.qiankubx.module.agreement.mapper.UserAgreementSignMapper;
import com.qiankubx.module.agreement.service.AgreementService;
import com.qiankubx.module.coupon.service.CouponService;
import com.qiankubx.module.promotion.service.AntiCheatService;
import com.qiankubx.module.promotion.service.InviteService;
import com.qiankubx.module.promotion.service.PointsService;
import com.qiankubx.module.promotion.service.PromoterLevelService;
import com.qiankubx.module.user.dto.*;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserService {

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private static final int TRIAL_DAYS_DEFAULT = 30;
    private static final int TRIAL_DAYS_INVITED = 45;
    private static final String TYPE_USER_AGREEMENT = "user_agreement";
    private static final String TYPE_PRIVACY_POLICY = "privacy_policy";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final AgreementVersionMapper agreementVersionMapper;
    private final UserAgreementSignMapper userAgreementSignMapper;
    private final AgreementService agreementService;
    private final PromoterLevelService promoterLevelService;
    private final CouponService couponService;
    private final InviteService inviteService;
    private final PointsService pointsService;
    private final AntiCheatService antiCheatService;

    public UserService(
            UserMapper userMapper,
            JwtUtil jwtUtil,
            StringRedisTemplate stringRedisTemplate,
            AgreementVersionMapper agreementVersionMapper,
            UserAgreementSignMapper userAgreementSignMapper,
            @Lazy AgreementService agreementService,
            @Lazy PromoterLevelService promoterLevelService,
            @Lazy CouponService couponService,
            @Lazy InviteService inviteService,
            @Lazy PointsService pointsService,
            @Lazy AntiCheatService antiCheatService
    ) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.agreementVersionMapper = agreementVersionMapper;
        this.userAgreementSignMapper = userAgreementSignMapper;
        this.agreementService = agreementService;
        this.promoterLevelService = promoterLevelService;
        this.couponService = couponService;
        this.inviteService = inviteService;
        this.pointsService = pointsService;
        this.antiCheatService = antiCheatService;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginVO smsLogin(SmsLoginDTO dto, String clientIp, String userAgent) {
        String cacheKey = SMS_CODE_PREFIX + dto.getPhone();
        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "验证码错误或已过期");
        }
        stringRedisTemplate.delete(cacheKey);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())
        );

        boolean isNew = false;
        if (user == null) {
            user = registerByPhone(dto.getPhone(), dto.getInviteCode(), clientIp, userAgent);
            isNew = true;
        }

        String token = jwtUtil.generateToken(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setIsNew(isNew);
        vo.setUser(toUserVO(user));
        vo.setMemberStatus(getMemberStatus(user.getId()));
        vo.setAgreementCheck(agreementService.checkAgreement(user.getId()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginVO wxLogin(WxLoginDTO dto, String clientIp, String userAgent) {
        String openid = "wx_" + dto.getCode();

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        boolean isNew = false;
        if (user == null) {
            user = registerByWx(openid, dto.getInviteCode(), clientIp, userAgent);
            isNew = true;
        }

        String token = jwtUtil.generateToken(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setIsNew(isNew);
        vo.setUser(toUserVO(user));
        vo.setMemberStatus(getMemberStatus(user.getId()));
        vo.setAgreementCheck(agreementService.checkAgreement(user.getId()));
        return vo;
    }

    public void sendSmsCode(String phone) {
        String cacheKey = SMS_CODE_PREFIX + phone;

        String existingCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (existingCode != null) {
            Long ttl = stringRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > (SMS_CODE_EXPIRE_MINUTES * 60 - 60)) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "验证码已发送，请稍后再试");
            }
        }

        String code = generateSmsCode();
        stringRedisTemplate.opsForValue().set(cacheKey, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("发送短信验证码: phone={}, code={}", phone, code);
    }

    public UserVO getProfile(Long userId) {
        User user = getById(userId);
        return toUserVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long userId, UserProfileDTO dto) {
        User user = getById(userId);

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);

        if (dto.getNickname() != null) {
            updateWrapper.set(User::getNickname, dto.getNickname());
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatarUrl() != null) {
            updateWrapper.set(User::getAvatarUrl, dto.getAvatarUrl());
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getCompany() != null) {
            updateWrapper.set(User::getCompany, dto.getCompany());
            user.setCompany(dto.getCompany());
        }
        if (dto.getDepartment() != null) {
            updateWrapper.set(User::getDepartment, dto.getDepartment());
            user.setDepartment(dto.getDepartment());
        }
        if (dto.getDefaultSubsidy() != null) {
            updateWrapper.set(User::getDefaultSubsidy, dto.getDefaultSubsidy());
            user.setDefaultSubsidy(dto.getDefaultSubsidy());
        }
        updateWrapper.set(User::getUpdatedAt, LocalDateTime.now());

        userMapper.update(null, updateWrapper);
        return toUserVO(user);
    }

    public MemberStatusVO getMemberStatus(Long userId) {
        User user = getById(userId);

        MemberStatusVO vo = new MemberStatusVO();
        vo.setMemberType(user.getMemberType());
        vo.setMemberStatus(user.getMemberStatus());
        vo.setMemberExpireTime(user.getMemberExpireTime());
        vo.setTrialEndTime(user.getTrialEndTime());

        LocalDateTime now = LocalDateTime.now();
        boolean isTrial = user.getMemberStatus() == 0
                && user.getTrialEndTime() != null
                && user.getTrialEndTime().isAfter(now);
        vo.setIsTrial(isTrial);

        boolean isExpired = false;
        if (user.getMemberStatus() == 1 && user.getMemberExpireTime() != null) {
            isExpired = user.getMemberExpireTime().isBefore(now);
        } else if (user.getMemberStatus() == 0 && user.getTrialEndTime() != null) {
            isExpired = user.getTrialEndTime().isBefore(now);
        }
        vo.setIsExpired(isExpired);

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId) {
        User user = getById(userId);

        user.setStatus(1);
        user.setPhone(null);
        user.setOpenid(null);
        user.setUnionid(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        stringRedisTemplate.delete("user:agreement:" + userId);
        stringRedisTemplate.delete("user:member:" + userId);

        log.info("用户注销账号: userId={}", userId);
    }

    private User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return user;
    }

    private User registerByPhone(String phone, String inviteCode, String clientIp, String userAgent) {
        User user = new User();
        user.setPhone(phone);
        user.setMemberType(0);
        user.setMemberStatus(0);
        user.setMonthlyInvoiceUsed(0);
        user.setMonthlyReimburseUsed(0);
        user.setDefaultSubsidy(BigDecimal.ZERO);
        user.setStatus(0);
        user.setInviteCode(generateInviteCode());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        processInviteCode(user, inviteCode);

        userMapper.insert(user);

        afterRegister(user, inviteCode, clientIp, userAgent);
        return user;
    }

    private User registerByWx(String openid, String inviteCode, String clientIp, String userAgent) {
        User user = new User();
        user.setOpenid(openid);
        user.setMemberType(0);
        user.setMemberStatus(0);
        user.setMonthlyInvoiceUsed(0);
        user.setMonthlyReimburseUsed(0);
        user.setDefaultSubsidy(BigDecimal.ZERO);
        user.setStatus(0);
        user.setInviteCode(generateInviteCode());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        processInviteCode(user, inviteCode);

        userMapper.insert(user);

        afterRegister(user, inviteCode, clientIp, userAgent);
        return user;
    }

    private void afterRegister(User user, String inviteCode, String clientIp, String userAgent) {
        Long userId = user.getId();

        signCurrentAgreements(user);

        promoterLevelService.getOrCreatePromoterLevel(userId);

        couponService.issueNewUserCoupon(userId);

        if (inviteCode != null && !inviteCode.isBlank()) {
            User inviter = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getInviteCode, inviteCode)
            );
            if (inviter != null) {
                try {
                    antiCheatService.assertInviteNotBlocked(inviter.getId(), clientIp, userAgent);
                } catch (Exception e) {
                    log.warn("邀请风控拦截，跳过绑定: userId={}, inviteCode={}, reason={}",
                            userId, inviteCode, e.getMessage());
                    return;
                }
            }

            inviteService.bindInviteRelation(userId, inviteCode);

            User freshUser = userMapper.selectById(userId);
            if (freshUser.getInviterId() != null) {
                pointsService.addPoints(freshUser.getInviterId(), 5, "invite_register", userId, "邀请好友注册");
                couponService.issueInviteCoupon(freshUser.getInviterId());
            }
        }

        stringRedisTemplate.opsForValue().set("user:agreement:" + userId, "true");
    }

    private void signCurrentAgreements(User user) {
        Long userId = user.getId();
        LocalDateTime now = LocalDateTime.now();

        AgreementVersion currentAgreement = findCurrentVersion(TYPE_USER_AGREEMENT);
        if (currentAgreement != null) {
            UserAgreementSign sign = new UserAgreementSign();
            sign.setUserId(userId);
            sign.setVersionId(currentAgreement.getId());
            sign.setAgreementType(TYPE_USER_AGREEMENT);
            sign.setVersionCode(currentAgreement.getVersionCode());
            sign.setIpAddress("register");
            sign.setSignedAt(now);
            userAgreementSignMapper.insert(sign);

            user.setAgreementVersionId(currentAgreement.getId());
            user.setAgreementSignedAt(now);
        }

        AgreementVersion currentPrivacy = findCurrentVersion(TYPE_PRIVACY_POLICY);
        if (currentPrivacy != null) {
            UserAgreementSign sign = new UserAgreementSign();
            sign.setUserId(userId);
            sign.setVersionId(currentPrivacy.getId());
            sign.setAgreementType(TYPE_PRIVACY_POLICY);
            sign.setVersionCode(currentPrivacy.getVersionCode());
            sign.setIpAddress("register");
            sign.setSignedAt(now);
            userAgreementSignMapper.insert(sign);

            user.setPrivacyVersionId(currentPrivacy.getId());
            user.setPrivacySignedAt(now);
        }

        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    private AgreementVersion findCurrentVersion(String type) {
        return agreementVersionMapper.selectOne(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getType, type)
                        .eq(AgreementVersion::getStatus, 1)
                        .le(AgreementVersion::getEffectiveAt, LocalDateTime.now())
                        .orderByDesc(AgreementVersion::getEffectiveAt)
                        .last("LIMIT 1")
        );
    }

    private void processInviteCode(User user, String inviteCode) {
        int trialDays = TRIAL_DAYS_DEFAULT;

        if (inviteCode != null && !inviteCode.isBlank()) {
            User inviter = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getInviteCode, inviteCode)
            );
            if (inviter != null) {
                user.setInviterId(inviter.getId());
                user.setRootInviterId(inviter.getInviterId());
                trialDays = TRIAL_DAYS_INVITED;
            }
        }

        user.setTrialEndTime(LocalDateTime.now().plusDays(trialDays));
    }

    private String generateInviteCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }
        String code = sb.toString();

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getInviteCode, code)
        );
        if (count > 0) {
            return generateInviteCode();
        }
        return code;
    }

    private String generateSmsCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCompany(user.getCompany());
        vo.setDepartment(user.getDepartment());
        vo.setInviteCode(user.getInviteCode());
        vo.setMemberType(user.getMemberType());
        vo.setMemberStatus(user.getMemberStatus());
        vo.setMemberExpireTime(user.getMemberExpireTime());
        vo.setTrialEndTime(user.getTrialEndTime());
        vo.setTeamId(user.getTeamId());
        vo.setMonthlyInvoiceUsed(user.getMonthlyInvoiceUsed());
        vo.setMonthlyReimburseUsed(user.getMonthlyReimburseUsed());
        vo.setDefaultSubsidy(user.getDefaultSubsidy());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
