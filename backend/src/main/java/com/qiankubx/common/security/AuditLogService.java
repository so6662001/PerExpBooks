package com.qiankubx.common.security;

import com.qiankubx.common.util.SnowflakeIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
            INSERT INTO t_audit_log (id, operator_id, operator_type, target_table, target_id,
                action, field_name, old_value, new_value, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Async
    public void log(Long operatorId, String operatorType, String targetTable,
                    Long targetId, String action, String fieldName,
                    String oldValue, String newValue) {
        try {
            long id = SnowflakeIdUtil.getInstance().nextId();
            jdbcTemplate.update(INSERT_SQL,
                    id, operatorId, operatorType, targetTable, targetId,
                    action, fieldName, oldValue, newValue, LocalDateTime.now());
        } catch (Exception e) {
            log.error("审计日志写入失败: operatorId={}, table={}, targetId={}, action={}",
                    operatorId, targetTable, targetId, action, e);
        }
    }
}
