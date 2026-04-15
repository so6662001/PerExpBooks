package com.qiankubx.common.interceptor;

import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
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
public class MemberInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

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

        LocalDateTime now = LocalDateTime.now();

        if (user.getMemberStatus() != null && user.getMemberStatus() == 1) {
            if (user.getMemberExpireTime() != null && user.getMemberExpireTime().isAfter(now)) {
                return true;
            }
        }

        if (user.getTrialEndTime() != null && user.getTrialEndTime().isAfter(now)) {
            return true;
        }

        throw new BizException(ResultCode.MEMBER_REQUIRED);
    }
}
