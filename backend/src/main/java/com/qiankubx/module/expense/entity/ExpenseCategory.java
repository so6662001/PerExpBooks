package com.qiankubx.module.expense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_expense_category")
public class ExpenseCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String icon;

    private Integer sortOrder;

    private Integer isSystem;

    private Long userId;

    private Integer status;
}
