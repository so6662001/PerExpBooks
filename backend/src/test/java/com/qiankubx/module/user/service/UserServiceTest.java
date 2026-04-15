package com.qiankubx.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.util.JwtUtil;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.mapper.AgreementVersionMapper;
import com.qiankubx.module.agreement.mapper.UserAgreementSignMapper;
import com.qiankubx.module.agreement.service.AgreementService;
import com.qiankubx.module.coupon.service.CouponService;
import com.qiankubx.module.promotion.service.AntiCheatService;
import com.qiankubx.module.promotion.service.InviteService;
import com.qiankubx.module.promotion.service.PointsService;
import com.qiankubx.module.promotion.service.PromoterLevelService;
import com.qiankubx.module.user.dto.LoginVO;
import com.qiankubx.module.user.dto.SmsLoginDTO;
import com.qiankubx.module.user.dto.UserVO;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private AgreementVersionMapper agreementVersionMapper;
    @Mock private UserAgreementSignMapper userAgreementSignMapper;
    @Mock private AgreementService agreementService;
    @Mock private PromoterLevelService promoterLevelService;
    @Mock private CouponService couponService;
    @Mock private InviteService inviteService;
    @Mock private PointsService pointsService;
    @Mock private AntiCheatService antiCheatService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userMapper, jwtUtil, stringRedisTemplate,
                agreementVersionMapper, userAgreementSignMapper,
                agreementService, promoterLevelService, couponService,
                inviteService, pointsService, antiCheatService
        );
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void smsLogin_newUser_shouldRegisterAndReturnToken() {
        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("123456");

        when(valueOps.get("sms:code:13800138000")).thenReturn("123456");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return 1;
        });
        when(jwtUtil.generateToken(1L)).thenReturn("jwt-token-123");
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "13800138000"));

        LoginVO result = userService.smsLogin(dto, "127.0.0.1", "TestAgent");

        assertThat(result.getToken()).isEqualTo("jwt-token-123");
        assertThat(result.getIsNew()).isTrue();
        assertThat(result.getUser()).isNotNull();
        verify(stringRedisTemplate).delete("sms:code:13800138000");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void smsLogin_existingUser_shouldLoginAndReturnToken() {
        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("123456");

        User existingUser = buildUser(1L, "13800138000");
        when(valueOps.get("sms:code:13800138000")).thenReturn("123456");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser);
        when(jwtUtil.generateToken(1L)).thenReturn("jwt-token-456");
        when(userMapper.selectById(1L)).thenReturn(existingUser);

        LoginVO result = userService.smsLogin(dto, "127.0.0.1", "TestAgent");

        assertThat(result.getToken()).isEqualTo("jwt-token-456");
        assertThat(result.getIsNew()).isFalse();
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void smsLogin_wrongCode_shouldThrowException() {
        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("000000");

        when(valueOps.get("sms:code:13800138000")).thenReturn("123456");

        assertThatThrownBy(() -> userService.smsLogin(dto, "127.0.0.1", "TestAgent"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("验证码错误或已过期");
    }

    @Test
    void smsLogin_expiredCode_shouldThrowException() {
        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("123456");

        when(valueOps.get("sms:code:13800138000")).thenReturn(null);

        assertThatThrownBy(() -> userService.smsLogin(dto, "127.0.0.1", "TestAgent"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("验证码错误或已过期");
    }

    @Test
    void smsLogin_withInviteCode_shouldSetTrialDays45() {
        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13900139000");
        dto.setCode("654321");
        dto.setInviteCode("ABC123");

        User inviter = buildUser(2L, "13700137000");
        inviter.setInviteCode("ABC123");

        when(valueOps.get("sms:code:13900139000")).thenReturn("654321");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null)
                .thenReturn(inviter)
                .thenReturn(inviter);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(3L);
            return 1;
        });
        when(jwtUtil.generateToken(3L)).thenReturn("jwt-token-inv");

        User registeredUser = buildUser(3L, "13900139000");
        registeredUser.setInviterId(2L);
        registeredUser.setTrialEndTime(LocalDateTime.now().plusDays(45));
        when(userMapper.selectById(3L)).thenReturn(registeredUser);

        LoginVO result = userService.smsLogin(dto, "127.0.0.1", "TestAgent");

        assertThat(result.getToken()).isEqualTo("jwt-token-inv");
        assertThat(result.getIsNew()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User inserted = captor.getValue();
        assertThat(inserted.getTrialEndTime()).isAfter(LocalDateTime.now().plusDays(44));
    }

    @Test
    void sendSmsCode_shouldStoreInRedis() {
        when(valueOps.get("sms:code:13800138000")).thenReturn(null);

        userService.sendSmsCode("13800138000");

        verify(valueOps).set(eq("sms:code:13800138000"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void sendSmsCode_tooFrequent_shouldThrowException() {
        when(valueOps.get("sms:code:13800138000")).thenReturn("123456");
        when(stringRedisTemplate.getExpire("sms:code:13800138000", TimeUnit.SECONDS)).thenReturn(280L);

        assertThatThrownBy(() -> userService.sendSmsCode("13800138000"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("验证码已发送，请稍后再试");
    }

    @Test
    void getProfile_shouldReturnUserVO() {
        User user = buildUser(1L, "13800138000");
        user.setNickname("测试用户");
        user.setCompany("测试公司");
        when(userMapper.selectById(1L)).thenReturn(user);

        UserVO result = userService.getProfile(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPhone()).isEqualTo("13800138000");
        assertThat(result.getNickname()).isEqualTo("测试用户");
        assertThat(result.getCompany()).isEqualTo("测试公司");
    }

    @Test
    void getProfile_userNotFound_shouldThrow() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> userService.getProfile(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void deleteAccount_shouldSoftDelete() {
        User user = buildUser(1L, "13800138000");
        user.setOpenid("wx_abc");
        user.setUnionid("union_abc");
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.deleteAccount(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(1);
        assertThat(updated.getPhone()).isNull();
        assertThat(updated.getOpenid()).isNull();
        assertThat(updated.getUnionid()).isNull();

        verify(stringRedisTemplate).delete("user:agreement:1");
        verify(stringRedisTemplate).delete("user:member:1");
    }

    private User buildUser(Long id, String phone) {
        User user = new User();
        user.setId(id);
        user.setPhone(phone);
        user.setMemberType(0);
        user.setMemberStatus(0);
        user.setMonthlyInvoiceUsed(0);
        user.setMonthlyReimburseUsed(0);
        user.setDefaultSubsidy(BigDecimal.ZERO);
        user.setStatus(0);
        user.setInviteCode("ABCDEF");
        user.setTrialEndTime(LocalDateTime.now().plusDays(30));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
