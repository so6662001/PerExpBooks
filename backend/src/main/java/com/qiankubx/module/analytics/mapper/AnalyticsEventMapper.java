package com.qiankubx.module.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiankubx.module.analytics.entity.AnalyticsEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsEventMapper extends BaseMapper<AnalyticsEvent> {

    @Select("SELECT COUNT(DISTINCT user_id) AS dau FROM t_analytics_event WHERE DATE(event_time) = CURDATE() AND user_id != 0")
    Long selectTodayDau();

    @Select("SELECT COUNT(DISTINCT user_id) AS cnt FROM t_analytics_event WHERE DATE(event_time) = CURDATE() AND event_name = 'first_open' AND user_id != 0")
    Long selectTodayNewUsers();

    @Select("SELECT COUNT(DISTINCT user_id) AS cnt FROM t_analytics_event WHERE DATE(event_time) = CURDATE() AND event_name = 'purchase' AND user_id != 0")
    Long selectTodayPaidUsers();

    @Select("""
            SELECT page_path AS pagePath,
                   COUNT(*) AS pv,
                   COUNT(DISTINCT user_id) AS uv
            FROM t_analytics_event
            WHERE DATE(event_time) >= #{startDate}
              AND DATE(event_time) <= #{endDate}
            GROUP BY page_path
            ORDER BY pv DESC
            """)
    List<Map<String, Object>> selectPageStats(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    @Select("""
            SELECT event_name AS eventName,
                   COUNT(*) AS count
            FROM t_analytics_event
            WHERE DATE(event_time) >= #{startDate}
              AND DATE(event_time) <= #{endDate}
            GROUP BY event_name
            ORDER BY count DESC
            """)
    List<Map<String, Object>> selectFeatureUsage(@Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);
}
