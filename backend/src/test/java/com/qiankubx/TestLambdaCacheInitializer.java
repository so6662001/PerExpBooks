package com.qiankubx;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * Initializes MyBatis-Plus lambda metadata cache for pure Mockito tests.
 * Without this, LambdaUpdateWrapper/LambdaQueryWrapper will fail with
 * "can not find lambda cache for this entity".
 */
public class TestLambdaCacheInitializer {

    private static boolean initialized = false;

    public static void initAll() {
        if (initialized) {
            return;
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.qiankubx");

        Class<?>[] entityClasses = {
                com.qiankubx.module.user.entity.User.class,
                com.qiankubx.module.expense.entity.Expense.class,
                com.qiankubx.module.expense.entity.BusinessTrip.class,
                com.qiankubx.module.expense.entity.ExpenseCategory.class,
                com.qiankubx.module.reimbursement.entity.Reimbursement.class,
                com.qiankubx.module.member.entity.MemberOrder.class,
                com.qiankubx.module.promotion.entity.Commission.class,
                com.qiankubx.module.promotion.entity.Invitation.class,
                com.qiankubx.module.promotion.entity.PointsLog.class,
                com.qiankubx.module.promotion.entity.PromoterLevel.class,
                com.qiankubx.module.promotion.entity.Withdrawal.class,
        };

        for (Class<?> clazz : entityClasses) {
            TableInfoHelper.initTableInfo(assistant, clazz);
        }

        initialized = true;
    }
}
