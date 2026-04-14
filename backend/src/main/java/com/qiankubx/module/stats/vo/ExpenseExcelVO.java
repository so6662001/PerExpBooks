package com.qiankubx.module.stats.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseExcelVO {

    @ExcelProperty("费用日期")
    private String expenseDate;

    @ExcelProperty("类别")
    private String categoryName;

    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    @ExcelProperty("发票号")
    private String invoiceNo;

    @ExcelProperty("发票类型")
    private String invoiceType;

    @ExcelProperty("销方名称")
    private String sellerName;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("报销状态")
    private String reimburseStatusName;
}
