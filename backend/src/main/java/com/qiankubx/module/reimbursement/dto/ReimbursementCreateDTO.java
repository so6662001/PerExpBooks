package com.qiankubx.module.reimbursement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReimbursementCreateDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotEmpty(message = "费用ID列表不能为空")
    private List<Long> expenseIds;

    private Long tripId;

    private String remark;
}
