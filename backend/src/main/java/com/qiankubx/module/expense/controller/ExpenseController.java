package com.qiankubx.module.expense.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.expense.dto.*;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.service.ExpenseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/upload-invoice")
    public Result<InvoiceUploadVO> uploadInvoice(HttpServletRequest request,
                                                  @RequestParam("file") MultipartFile file) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(expenseService.uploadInvoice(userId, file));
    }

    @PostMapping
    public Result<Expense> create(HttpServletRequest request,
                                  @Valid @RequestBody ExpenseCreateDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(expenseService.createExpense(userId, dto));
    }

    @PutMapping("/{id}")
    public Result<Expense> update(HttpServletRequest request,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ExpenseUpdateDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        dto.setId(id);
        return Result.ok(expenseService.updateExpense(userId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request,
                               @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        expenseService.deleteExpense(userId, id);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<IPage<ExpenseVO>> list(HttpServletRequest request,
                                         ExpenseQueryDTO queryDTO) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(expenseService.listExpenses(userId, queryDTO));
    }

    @GetMapping("/{id}")
    public Result<ExpenseVO> detail(HttpServletRequest request,
                                    @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(expenseService.getExpense(userId, id));
    }

    @GetMapping("/pending")
    public Result<List<ExpenseVO>> pending(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(expenseService.listPending(userId));
    }
}
