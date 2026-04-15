package com.qiankubx.module.user.controller;

import com.qiankubx.common.response.Result;
import com.qiankubx.module.user.dto.LoginVO;
import com.qiankubx.module.user.dto.SendSmsDTO;
import com.qiankubx.module.user.dto.SmsLoginDTO;
import com.qiankubx.module.user.dto.WxLoginDTO;
import com.qiankubx.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/sms-login")
    public Result<LoginVO> smsLogin(HttpServletRequest request, @Valid @RequestBody SmsLoginDTO dto) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        return Result.ok(userService.smsLogin(dto, clientIp, userAgent));
    }

    @PostMapping("/wx-login")
    public Result<LoginVO> wxLogin(HttpServletRequest request, @Valid @RequestBody WxLoginDTO dto) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        return Result.ok(userService.wxLogin(dto, clientIp, userAgent));
    }

    @PostMapping("/send-sms")
    public Result<Void> sendSms(@Valid @RequestBody SendSmsDTO dto) {
        userService.sendSmsCode(dto.getPhone());
        return Result.ok();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
