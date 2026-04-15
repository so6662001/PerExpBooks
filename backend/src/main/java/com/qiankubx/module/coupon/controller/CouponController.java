package com.qiankubx.module.coupon.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.coupon.dto.UserCouponVO;
import com.qiankubx.module.coupon.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/my")
    public Result<List<UserCouponVO>> myCoupons(HttpServletRequest request,
                                                 @RequestParam(required = false) Integer status) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(couponService.listMyCoupons(userId, status));
    }

    @GetMapping("/available")
    public Result<List<UserCouponVO>> available(HttpServletRequest request,
                                                 @RequestParam Integer planType) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(couponService.getAvailableCoupons(userId, planType));
    }

    @PostMapping("/receive/{templateId}")
    public Result<Void> receive(HttpServletRequest request,
                                 @PathVariable Long templateId) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        couponService.receiveCoupon(userId, templateId);
        return Result.ok();
    }
}
