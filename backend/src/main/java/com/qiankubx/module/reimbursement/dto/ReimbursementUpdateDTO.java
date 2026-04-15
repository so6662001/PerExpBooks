package com.qiankubx.module.reimbursement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReimbursementUpdateDTO {

    @NotNull(message = "ID不能为空")
    private Long id;

    private String title;

    private List<Long> expenseIds;

    private String remark;
}
