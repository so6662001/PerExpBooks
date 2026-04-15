package com.qiankubx.module.trigger.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.trigger.service.ConversionGuideService;
import com.qiankubx.module.trigger.service.SceneTriggerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/trigger")
@RequiredArgsConstructor
public class TriggerController {

    private final SceneTriggerService sceneTriggerService;
    private final ConversionGuideService conversionGuideService;

    @GetMapping("/check")
    public Result<Map<String, Object>> check(HttpServletRequest request, @RequestParam String scene) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(sceneTriggerService.checkTriggers(userId, scene));
    }

    @GetMapping("/conversion")
    public Result<Map<String, Object>> conversion(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(conversionGuideService.checkConversion(userId));
    }

    @GetMapping("/conversion/scene")
    public Result<Map<String, Object>> conversionScene(HttpServletRequest request, @RequestParam String scene) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(conversionGuideService.checkSceneTrigger(userId, scene));
    }
}
