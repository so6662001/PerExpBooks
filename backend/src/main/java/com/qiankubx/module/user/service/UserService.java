package com.qiankubx.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.common.util.JwtUtil;
import com.qiankubx.module.user.dto.*;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private static final int TRIAL_DAYS_DEFAULT = 30;
    private static final int TRIAL_DAYS_INVITED = 45;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public LoginVO smsLogin(SmsLoginDTO dto) {
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
            user = registerByPhone(dto.getPhone(), dto.getInviteCode());
            isNew = true;
        }

        String token = jwtUtil.generateToken(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setIsNew(isNew);
        vo.setUser(toUserVO(user));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginVO wxLogin(WxLoginDTO dto) {
        // Simulated WeChat login: use code as openid for demo purposes
        String openid = "wx_" + dto.getCode();

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        boolean isNew = false;
        if (user == null) {
            user = registerByWx(openid, dto.getInviteCode());
            isNew = true;
        }

        String token = jwtUtil.generateToken(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setIsNew(isNew);
        vo.setUser(toUserVO(user));
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

    private User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return user;
    }

    private User registerByPhone(String phone, String inviteCode) {
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
        return user;
    }

    private User registerByWx(String openid, String inviteCode) {
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
        return user;
    }

    private void processInviteCode(User user, String inviteCode) {
        int trialDays = TRIAL_DAYS_DEFAULT;

        if (inviteCode != null && !inviteCode.isBlank()) {
            User inviter = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getInviteCode, inviteCode)
            );
            if (inviter != null) {
                user.setInviterId(inviter.getId());
                user.setRootInviterId(
                        inviter.getRootInviterId() != null ? inviter.getRootInviterId() : inviter.getId()
                );
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
