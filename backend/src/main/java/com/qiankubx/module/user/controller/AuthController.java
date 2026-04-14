package com.qiankubx.module.user.controller;

import com.qiankubx.common.response.Result;
import com.qiankubx.module.user.dto.LoginVO;
import com.qiankubx.module.user.dto.SendSmsDTO;
import com.qiankubx.module.user.dto.SmsLoginDTO;
import com.qiankubx.module.user.dto.WxLoginDTO;
import com.qiankubx.module.user.service.UserService;
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
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        return Result.ok(userService.smsLogin(dto));
    }

    @PostMapping("/wx-login")
    public Result<LoginVO> wxLogin(@Valid @RequestBody WxLoginDTO dto) {
        return Result.ok(userService.wxLogin(dto));
    }

    @PostMapping("/send-sms")
    public Result<Void> sendSms(@Valid @RequestBody SendSmsDTO dto) {
        userService.sendSmsCode(dto.getPhone());
        return Result.ok();
    }
}
