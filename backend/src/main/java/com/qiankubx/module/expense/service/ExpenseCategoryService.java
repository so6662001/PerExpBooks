package com.qiankubx.module.expense.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.module.expense.entity.ExpenseCategory;
import com.qiankubx.module.expense.mapper.ExpenseCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryMapper categoryMapper;

    public List<ExpenseCategory> listCategories(Long userId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ExpenseCategory>()
                        .eq(ExpenseCategory::getStatus, 0)
                        .and(w -> w.eq(ExpenseCategory::getIsSystem, 1)
                                .or()
                                .eq(ExpenseCategory::getUserId, userId))
                        .orderByAsc(ExpenseCategory::getSortOrder)
        );
    }

    public ExpenseCategory createCategory(Long userId, String name, String icon) {
        ExpenseCategory category = new ExpenseCategory();
        category.setName(name);
        category.setIcon(icon);
        category.setIsSystem(0);
        category.setUserId(userId);
        category.setSortOrder(100);
        category.setStatus(0);
        categoryMapper.insert(category);
        return category;
    }

    public void deleteCategory(Long userId, Long categoryId) {
        ExpenseCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }
        if (category.getIsSystem() == 1) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "系统分类不可删除");
        }
        category.setStatus(1);
        categoryMapper.updateById(category);
    }
}
