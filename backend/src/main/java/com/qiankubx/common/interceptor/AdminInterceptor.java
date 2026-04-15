package com.qiankubx.common.interceptor;

import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        String adminIds = System.getenv("ADMIN_USER_IDS");
        if (adminIds == null || adminIds.isBlank()) {
            adminIds = "1";
        }

        boolean isAdmin = Arrays.stream(adminIds.split(","))
                .anyMatch(id -> id.trim().equals(userId.toString()));

        if (!isAdmin) {
            throw new BizException(ResultCode.FORBIDDEN);
        }

        return true;
    }
}
