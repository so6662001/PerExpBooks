package com.qiankubx.module.share.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.share.dto.ShareLogDTO;
import com.qiankubx.module.share.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/log")
    public Result<Void> logShare(HttpServletRequest request,
                                 @Valid @RequestBody ShareLogDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        shareService.logShare(userId, dto);
        return Result.ok();
    }

    @GetMapping("/templates")
    public Result<List<Map<String, String>>> getShareTemplates() {
        return Result.ok(shareService.getShareTemplates());
    }
}
