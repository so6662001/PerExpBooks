package com.qiankubx.module.analytics.controller;

import com.qiankubx.common.response.Result;
import com.qiankubx.module.analytics.dto.ErrorReportDTO;
import com.qiankubx.module.analytics.dto.EventReportDTO;
import com.qiankubx.module.analytics.dto.PerformanceReportDTO;
import com.qiankubx.module.analytics.service.EventCollectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsReportController {

    private final EventCollectService eventCollectService;

    @PostMapping("/report")
    public Result<Void> reportEvents(HttpServletRequest request,
                                     @Valid @RequestBody EventReportDTO dto) {
        Long userId = getUserId(request);
        eventCollectService.reportEvents(userId, dto);
        return Result.ok();
    }

    @PostMapping("/performance")
    public Result<Void> reportPerformance(HttpServletRequest request,
                                          @Valid @RequestBody PerformanceReportDTO dto) {
        Long userId = getUserId(request);
        eventCollectService.reportPerformance(userId, dto);
        return Result.ok();
    }

    @PostMapping("/error")
    public Result<Void> reportError(HttpServletRequest request,
                                    @Valid @RequestBody ErrorReportDTO dto) {
        Long userId = getUserId(request);
        eventCollectService.reportErrors(userId, dto);
        return Result.ok();
    }

    private Long getUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return 0L;
    }
}
