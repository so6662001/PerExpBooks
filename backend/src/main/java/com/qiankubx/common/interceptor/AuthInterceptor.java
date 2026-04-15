package com.qiankubx.common.interceptor;

import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    private static final String HEADER_AUTH = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(HEADER_AUTH);
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());
        try {
            Long userId = jwtUtil.getUserId(token);
            request.setAttribute(ATTR_USER_ID, userId);
            return true;
        } catch (Exception e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }
}
