package com.qiankubx.module.promotion.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.promotion.dto.BalanceVO;
import com.qiankubx.module.promotion.dto.WithdrawDTO;
import com.qiankubx.module.promotion.dto.WithdrawalVO;
import com.qiankubx.module.promotion.service.WithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/withdrawal")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping("/apply")
    public Result<Void> apply(HttpServletRequest request,
                               @Valid @RequestBody WithdrawDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        withdrawalService.applyWithdraw(userId, dto.getAmount(), dto.getWithdrawType(), dto.getAccountInfo());
        return Result.ok();
    }

    @GetMapping("/records")
    public Result<List<WithdrawalVO>> records(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(withdrawalService.listWithdrawals(userId));
    }

    @GetMapping("/balance")
    public Result<BalanceVO> balance(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(withdrawalService.getBalance(userId));
    }
}
