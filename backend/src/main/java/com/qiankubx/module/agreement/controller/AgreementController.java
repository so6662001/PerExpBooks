package com.qiankubx.module.agreement.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.agreement.dto.*;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.service.AgreementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agreement")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @GetMapping("/check")
    public Result<AgreementCheckVO> check(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(agreementService.checkAgreement(userId));
    }

    @GetMapping("/current/{type}")
    public Result<AgreementVersionVO> getCurrentVersion(@PathVariable String type) {
        AgreementVersion version = agreementService.getCurrentVersion(type);
        if (version == null) {
            return Result.ok(null);
        }
        return Result.ok(agreementService.getVersion(version.getId()));
    }

    @PostMapping("/sign")
    public Result<Void> sign(HttpServletRequest request,
                             @Valid @RequestBody AgreementSignDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        agreementService.signAgreement(userId, dto, request);
        return Result.ok();
    }

    @GetMapping("/history/{type}")
    public Result<List<AgreementVersionVO>> history(@PathVariable String type) {
        return Result.ok(agreementService.getHistory(type));
    }

    @GetMapping("/version/{id}")
    public Result<AgreementVersionVO> version(@PathVariable Long id) {
        return Result.ok(agreementService.getVersion(id));
    }

    @GetMapping("/my-signs")
    public Result<List<UserAgreementSignVO>> mySigns(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(agreementService.getMySigns(userId));
    }
}
