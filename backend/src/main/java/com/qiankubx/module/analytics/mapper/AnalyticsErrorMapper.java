package com.qiankubx.module.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiankubx.module.analytics.entity.AnalyticsError;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsErrorMapper extends BaseMapper<AnalyticsError> {

    @Select("""
            SELECT error_type AS errorType,
                   error_message AS errorMessage,
                   page_path AS pagePath,
                   platform,
                   os,
                   COUNT(*) AS count,
                   MAX(event_time) AS lastOccurrence
            FROM t_analytics_error
            WHERE DATE(event_time) >= #{startDate}
              AND DATE(event_time) <= #{endDate}
            GROUP BY error_type, error_message, page_path, platform, os
            ORDER BY count DESC
            LIMIT 100
            """)
    List<Map<String, Object>> selectErrorList(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);
}
