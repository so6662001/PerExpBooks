package com.qiankubx.module.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiankubx.module.analytics.entity.AnalyticsApi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsApiMapper extends BaseMapper<AnalyticsApi> {

    @Select("""
            SELECT api_path AS apiPath,
                   method,
                   AVG(duration_ms) AS avgDuration,
                   MAX(duration_ms) AS maxDuration,
                   COUNT(*) AS callCount
            FROM t_analytics_api
            WHERE DATE(event_time) >= #{startDate}
              AND DATE(event_time) <= #{endDate}
            GROUP BY api_path, method
            ORDER BY avgDuration DESC
            LIMIT 50
            """)
    List<Map<String, Object>> selectSlowApis(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate);
}
