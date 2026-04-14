package com.qiankubx.module.promotion.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.promotion.dto.*;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import com.qiankubx.module.promotion.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final InviteService inviteService;
    private final CommissionService commissionService;
    private final PointsService pointsService;
    private final PromoterLevelService promoterLevelService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);

        LevelInfoVO levelInfo = promoterLevelService.getLevelInfo(userId);
        PromoterLevel level = promoterLevelService.getOrCreatePromoterLevel(userId);

        DashboardVO vo = new DashboardVO();
        vo.setLevelInfo(levelInfo);
        vo.setTotalCommission(level.getTotalCommission());
        vo.setAvailableBalance(level.getAvailableBalance());
        vo.setFrozenBalance(level.getFrozenBalance());
        vo.setPoints(level.getPoints());
        vo.setTotalInvite(level.getInviteCount());
        vo.setPaidInvite(level.getPaidInviteCount());

        return Result.ok(vo);
    }

    @GetMapping("/invite-code")
    public Result<InviteCodeVO> getInviteCode(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(inviteService.getInviteCode(userId));
    }

    @GetMapping("/invite-records")
    public Result<List<InviteRecordVO>> inviteRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(inviteService.listInviteRecords(userId));
    }

    @GetMapping("/commission")
    public Result<List<CommissionVO>> commission(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(commissionService.listCommissions(userId));
    }

    @GetMapping("/points-log")
    public Result<List<PointsLogVO>> pointsLog(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(pointsService.getPointsLog(userId));
    }

    @PostMapping("/points/redeem")
    public Result<Void> redeemPoints(HttpServletRequest request,
                                      @Valid @RequestBody RedeemDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        pointsService.redeemPoints(userId, dto.getRedeemType());
        return Result.ok();
    }

    @GetMapping("/level-info")
    public Result<LevelInfoVO> levelInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(promoterLevelService.getLevelInfo(userId));
    }
}
