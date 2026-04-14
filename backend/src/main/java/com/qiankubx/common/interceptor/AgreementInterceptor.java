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
public class AgreementInterceptor implements HandlerInterceptor {

    private static final String AGREEMENT_KEY_PREFIX = "user:agreement:";

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

        Boolean agreed = (Boolean) redisTemplate.opsForValue().get(AGREEMENT_KEY_PREFIX + userId);
        if (agreed == null || !agreed) {
            throw new BizException(ResultCode.AGREEMENT_REQUIRED);
        }
        return true;
    }
}
