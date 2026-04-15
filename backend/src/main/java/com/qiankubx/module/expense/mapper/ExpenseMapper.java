package com.qiankubx.module.expense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiankubx.module.expense.dto.ExpenseQueryDTO;
import com.qiankubx.module.expense.dto.ExpenseVO;
import com.qiankubx.module.expense.entity.Expense;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExpenseMapper extends BaseMapper<Expense> {

    @Select("<script>" +
            "SELECT e.*, ec.name AS category_name " +
            "FROM t_expense e " +
            "LEFT JOIN t_expense_category ec ON e.category_id = ec.id " +
            "WHERE e.user_id = #{userId} AND e.status = 0 " +
            "<if test='params.categoryId != null'> AND e.category_id = #{params.categoryId}</if>" +
            "<if test='params.type != null'> AND e.type = #{params.type}</if>" +
            "<if test='params.reimburseStatus != null'> AND e.reimburse_status = #{params.reimburseStatus}</if>" +
            "<if test='params.startDate != null'> AND e.expense_date &gt;= #{params.startDate}</if>" +
            "<if test='params.endDate != null'> AND e.expense_date &lt;= #{params.endDate}</if>" +
            " ORDER BY e.created_at DESC" +
            "</script>")
    IPage<ExpenseVO> selectByUserIdWithPage(Page<?> page,
                                            @Param("userId") Long userId,
                                            @Param("params") ExpenseQueryDTO params);

    @Select("SELECT e.*, ec.name AS category_name " +
            "FROM t_expense e " +
            "LEFT JOIN t_expense_category ec ON e.category_id = ec.id " +
            "WHERE e.user_id = #{userId} AND e.status = 0 AND e.reimburse_status = 0 " +
            "ORDER BY e.created_at DESC")
    List<ExpenseVO> selectPendingByUserId(@Param("userId") Long userId);
}
