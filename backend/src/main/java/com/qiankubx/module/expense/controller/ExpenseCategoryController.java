package com.qiankubx.module.expense.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.expense.entity.ExpenseCategory;
import com.qiankubx.module.expense.service.ExpenseCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/expense-category")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    @GetMapping("/list")
    public Result<List<ExpenseCategory>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(categoryService.listCategories(userId));
    }

    @PostMapping("/create")
    public Result<ExpenseCategory> create(HttpServletRequest request,
                                          @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        String name = body.get("name");
        String icon = body.get("icon");
        return Result.ok(categoryService.createCategory(userId, name, icon));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request,
                               @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        categoryService.deleteCategory(userId, id);
        return Result.ok();
    }
}
