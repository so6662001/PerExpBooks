package com.qiankubx.module.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiankubx.module.promotion.entity.PromoterLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PromoterLevelMapper extends BaseMapper<PromoterLevel> {

    @Update("UPDATE t_promoter_level SET frozen_balance = frozen_balance + #{amount}, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int incrementFrozenBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_promoter_level SET frozen_balance = GREATEST(frozen_balance - #{amount}, 0), " +
            "available_balance = available_balance + #{amount}, " +
            "total_commission = total_commission + #{amount}, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int settleFrozenToAvailable(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_promoter_level SET available_balance = available_balance - #{amount}, " +
            "withdrawn_amount = withdrawn_amount + #{amount}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND available_balance >= #{amount}")
    int deductAvailableBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_promoter_level SET frozen_balance = GREATEST(frozen_balance - #{amount}, 0), " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int decrementFrozenBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_promoter_level SET available_balance = GREATEST(available_balance - #{amount}, 0), " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int decrementAvailableBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_promoter_level SET points = points + #{points}, " +
            "total_points = total_points + #{absPoints}, updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    int incrementPoints(@Param("userId") Long userId, @Param("points") int points, @Param("absPoints") int absPoints);
}
