package com.qiankubx.common.interceptor;

import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberInterceptor implements HandlerInterceptor {

    private static final String MEMBER_KEY_PREFIX = "user:member:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (userId == null) {
            return true;
        }

        Boolean isMember = (Boolean) redisTemplate.opsForValue().get(MEMBER_KEY_PREFIX + userId);
        if (isMember == null || !isMember) {
            throw new BizException(ResultCode.MEMBER_REQUIRED);
        }
        return true;
    }
}
