package com.qiankubx.module.member.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.member.dto.*;
import com.qiankubx.module.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/plans")
    public Result<List<PlanVO>> getPlans() {
        return Result.ok(memberService.getPlans());
    }

    @GetMapping("/status")
    public Result<MemberStatusVO> getStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(memberService.getMemberStatus(userId));
    }

    @PostMapping("/create-order")
    public Result<OrderVO> createOrder(HttpServletRequest request,
                                       @Valid @RequestBody CreateOrderDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(memberService.createOrder(userId, dto));
    }

    @PostMapping("/wx-notify")
    public Result<Void> wxNotify(@RequestParam String orderNo,
                                  @RequestParam(required = false) String tradeNo) {
        log.info("[模拟微信支付回调] orderNo={}, tradeNo={}", orderNo, tradeNo);
        String simulatedTradeNo = tradeNo != null ? tradeNo : "WX" + System.currentTimeMillis();
        memberService.handlePaySuccess(orderNo, simulatedTradeNo);
        return Result.ok();
    }

    @PostMapping("/ali-notify")
    public Result<Void> aliNotify(@RequestParam String orderNo,
                                   @RequestParam(required = false) String tradeNo) {
        log.info("[模拟支付宝回调] orderNo={}, tradeNo={}", orderNo, tradeNo);
        String simulatedTradeNo = tradeNo != null ? tradeNo : "ALI" + System.currentTimeMillis();
        memberService.handlePaySuccess(orderNo, simulatedTradeNo);
        return Result.ok();
    }

    @GetMapping("/order/{orderNo}")
    public Result<OrderVO> getOrder(HttpServletRequest request,
                                     @PathVariable String orderNo) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(memberService.getOrderDetail(userId, orderNo));
    }

    @GetMapping("/orders")
    public Result<List<OrderVO>> listOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(memberService.listOrders(userId));
    }

    @PostMapping("/refund/{orderNo}")
    public Result<Void> refund(HttpServletRequest request, @PathVariable String orderNo) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        memberService.applyRefund(userId, orderNo);
        return Result.ok();
    }
}
