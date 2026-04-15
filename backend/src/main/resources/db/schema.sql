-- ============================================================
-- 钱库 (QianKu) 数据库初始化脚本
-- 数据库: qianku_db
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `qianku_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `qianku_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------
-- 1. t_user - 用户表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `openid`                 VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '微信openid',
    `unionid`                VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '微信unionid',
    `phone`                  VARCHAR(20)   NOT NULL DEFAULT '' COMMENT '手机号',
    `nickname`               VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar_url`             VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '头像URL',
    `company`                VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '公司名称',
    `department`             VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '部门',
    `invite_code`            VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '我的邀请码',
    `inviter_id`             BIGINT        NOT NULL DEFAULT 0 COMMENT '一级邀请人用户ID',
    `root_inviter_id`        BIGINT        NOT NULL DEFAULT 0 COMMENT '二级邀请人用户ID',
    `member_type`            TINYINT       NOT NULL DEFAULT 0 COMMENT '会员类型: 0免费 1月度 2年度 3团队',
    `member_status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '会员状态: 0免费版 1有效 2已过期',
    `member_expire_time`     DATETIME      NULL DEFAULT NULL COMMENT '会员到期时间',
    `trial_end_time`         DATETIME      NULL DEFAULT NULL COMMENT '试用到期时间',
    `team_id`                BIGINT        NOT NULL DEFAULT 0 COMMENT '所属团队ID',
    `monthly_invoice_used`   INT           NOT NULL DEFAULT 0 COMMENT '本月已用发票上传次数',
    `monthly_reimburse_used` INT           NOT NULL DEFAULT 0 COMMENT '本月已用报销单次数',
    `default_subsidy`        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '默认补贴标准(元/天)',
    `agreement_version_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '最后签署的用户协议版本ID',
    `privacy_version_id`     BIGINT        NOT NULL DEFAULT 0 COMMENT '最后签署的隐私政策版本ID',
    `agreement_signed_at`    DATETIME      NULL DEFAULT NULL COMMENT '最后签署用户协议时间',
    `privacy_signed_at`      DATETIME      NULL DEFAULT NULL COMMENT '最后签署隐私政策时间',
    `status`                 TINYINT       NOT NULL DEFAULT 0 COMMENT '账号状态: 0正常 1禁用',
    `created_at`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    KEY `idx_unionid` (`unionid`),
    KEY `idx_phone` (`phone`),
    KEY `idx_inviter_id` (`inviter_id`),
    KEY `idx_root_inviter_id` (`root_inviter_id`),
    KEY `idx_team_id` (`team_id`),
    KEY `idx_member_status` (`member_status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. t_agreement_version - 协议版本表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_agreement_version`;
CREATE TABLE `t_agreement_version` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `agreement_type`      TINYINT       NOT NULL DEFAULT 0 COMMENT '协议类型: 1用户服务协议 2隐私政策',
    `version_code`        VARCHAR(10)   NOT NULL DEFAULT '' COMMENT '版本号',
    `version_seq`         INT           NOT NULL DEFAULT 0 COMMENT '版本序号',
    `title`               VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '协议标题',
    `content`             LONGTEXT      NULL COMMENT '协议全文',
    `change_level`        TINYINT       NOT NULL DEFAULT 1 COMMENT '变更等级: 1普通变更 2重大变更',
    `change_summary`      VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '变更摘要',
    `change_detail`       TEXT          NULL COMMENT '详细变更说明',
    `effective_date`      DATETIME      NULL DEFAULT NULL COMMENT '计划生效日期',
    `publish_date`        DATETIME      NULL DEFAULT NULL COMMENT '发布日期',
    `status`              TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1待生效 2生效中 3已归档',
    `notify_days_before`  INT           NOT NULL DEFAULT 15 COMMENT '提前通知天数',
    `created_by`          BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人',
    `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_agreement_type` (`agreement_type`),
    KEY `idx_status` (`status`),
    KEY `idx_effective_date` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='协议版本表';

-- -----------------------------------------------------------
-- 3. t_user_agreement_sign - 用户协议签署记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_user_agreement_sign`;
CREATE TABLE `t_user_agreement_sign` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '用户ID',
    `agreement_type`  TINYINT      NOT NULL DEFAULT 0 COMMENT '协议类型: 1用户服务协议 2隐私政策',
    `version_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '协议版本ID',
    `version_code`    VARCHAR(10)  NOT NULL DEFAULT '' COMMENT '版本号',
    `sign_action`     TINYINT      NOT NULL DEFAULT 0 COMMENT '签署动作: 1首次注册签署 2更新后重新确认',
    `sign_ip`         VARCHAR(45)  NOT NULL DEFAULT '' COMMENT '签署IP',
    `sign_device`     VARCHAR(200) NOT NULL DEFAULT '' COMMENT '签署设备信息',
    `sign_platform`   VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '签署平台',
    `user_agent`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'User-Agent',
    `data_sign`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'HMAC签名',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签署时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_version_id` (`version_id`),
    KEY `idx_user_type` (`user_id`, `agreement_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户协议签署记录表';

-- -----------------------------------------------------------
-- 4. t_expense_category - 费用分类表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_expense_category`;
CREATE TABLE `t_expense_category` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(30) NOT NULL DEFAULT '' COMMENT '分类名称',
    `icon`       VARCHAR(50) NOT NULL DEFAULT '' COMMENT '图标',
    `sort_order` INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `is_system`  TINYINT     NOT NULL DEFAULT 0 COMMENT '是否系统预设: 0否 1是',
    `user_id`    BIGINT      NOT NULL DEFAULT 0 COMMENT '用户ID(自定义分类)',
    `status`     TINYINT     NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1禁用',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_system` (`is_system`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用分类表';

-- -----------------------------------------------------------
-- 5. t_business_trip - 出差记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_business_trip`;
CREATE TABLE `t_business_trip` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT        NOT NULL DEFAULT 0 COMMENT '用户ID',
    `title`           VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '出差标题',
    `destination`     VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '目的地',
    `start_date`      DATE          NULL DEFAULT NULL COMMENT '开始日期',
    `end_date`        DATE          NULL DEFAULT NULL COMMENT '结束日期',
    `days`            INT           NOT NULL DEFAULT 0 COMMENT '出差天数',
    `subsidy_per_day` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '每日补贴(元)',
    `subsidy_total`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '补贴合计(元)',
    `remark`          VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '备注',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1删除',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_start_date` (`start_date`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出差记录表';

-- -----------------------------------------------------------
-- 6. t_expense - 费用记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_expense`;
CREATE TABLE `t_expense` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT        NOT NULL DEFAULT 0 COMMENT '用户ID',
    `category_id`      BIGINT        NOT NULL DEFAULT 0 COMMENT '费用分类ID',
    `trip_id`          BIGINT        NULL DEFAULT NULL COMMENT '出差记录ID',
    `type`             TINYINT       NOT NULL DEFAULT 0 COMMENT '费用类型: 1发票费用 2出差补贴 3手动录入',
    `amount`           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    `tax_amount`       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '税额',
    `invoice_no`       VARCHAR(30)   NOT NULL DEFAULT '' COMMENT '发票号码',
    `invoice_code`     VARCHAR(20)   NOT NULL DEFAULT '' COMMENT '发票代码',
    `invoice_date`     DATE          NULL DEFAULT NULL COMMENT '开票日期',
    `invoice_type`     VARCHAR(30)   NOT NULL DEFAULT '' COMMENT '发票类型',
    `seller_name`      VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '销方名称',
    `buyer_name`       VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '购方名称',
    `file_url`         VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '附件URL',
    `file_name`        VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '附件文件名',
    `description`      VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '描述',
    `expense_date`     DATE          NULL DEFAULT NULL COMMENT '费用日期',
    `reimburse_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '报销状态: 0待报销 1已提交 2已收款',
    `reimbursement_id` BIGINT        NULL DEFAULT NULL COMMENT '报销单ID',
    `data_sign`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '数据签名',
    `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1删除',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_trip_id` (`trip_id`),
    KEY `idx_reimbursement_id` (`reimbursement_id`),
    KEY `idx_reimburse_status` (`reimburse_status`),
    KEY `idx_invoice_no` (`invoice_no`),
    KEY `idx_expense_date` (`expense_date`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用记录表';

-- -----------------------------------------------------------
-- 7. t_reimbursement - 报销单表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_reimbursement`;
CREATE TABLE `t_reimbursement` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT        NOT NULL DEFAULT 0 COMMENT '用户ID',
    `reimburse_no`     VARCHAR(20)   NOT NULL DEFAULT '' COMMENT '报销单号',
    `title`            VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '报销标题',
    `total_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    `invoice_count`    INT           NOT NULL DEFAULT 0 COMMENT '发票数量',
    `item_count`       INT           NOT NULL DEFAULT 0 COMMENT '费用条目数',
    `remark`           VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '备注',
    `pdf_url`          VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '报销单PDF',
    `merged_pdf_url`   VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '合并PDF',
    `zip_url`          VARCHAR(500)  NOT NULL DEFAULT '' COMMENT 'ZIP包下载',
    `trip_id`          BIGINT        NULL DEFAULT NULL COMMENT '出差记录ID',
    `reimburse_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '报销状态: 0已生成 1已导出 2已收款',
    `exported_at`      DATETIME      NULL DEFAULT NULL COMMENT '导出时间',
    `export_count`     INT           NOT NULL DEFAULT 0 COMMENT '导出次数',
    `email_sent`       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已发邮件: 0否 1是',
    `email_address`    VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '发送邮箱地址',
    `email_sent_at`    DATETIME      NULL DEFAULT NULL COMMENT '邮件发送时间',
    `received_at`      DATETIME      NULL DEFAULT NULL COMMENT '收款时间',
    `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1删除',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reimburse_no` (`reimburse_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_trip_id` (`trip_id`),
    KEY `idx_reimburse_status` (`reimburse_status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销单表';

-- -----------------------------------------------------------
-- 8. t_member_order - 会员订单表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_member_order`;
CREATE TABLE `t_member_order` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT        NOT NULL DEFAULT 0 COMMENT '用户ID',
    `order_no`        VARCHAR(32)   NOT NULL DEFAULT '' COMMENT '订单号',
    `plan_type`       TINYINT       NOT NULL DEFAULT 0 COMMENT '套餐类型: 1月度 2年度 3团队',
    `original_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `pay_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    `coupon_id`       BIGINT        NULL DEFAULT NULL COMMENT '优惠券ID',
    `pay_type`        TINYINT       NOT NULL DEFAULT 0 COMMENT '支付方式: 1微信 2支付宝',
    `pay_status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '支付状态: 0未支付 1已支付 2已退款',
    `pay_time`        DATETIME      NULL DEFAULT NULL COMMENT '支付时间',
    `trade_no`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '第三方交易号',
    `member_start`    DATETIME      NULL DEFAULT NULL COMMENT '会员开始时间',
    `member_end`      DATETIME      NULL DEFAULT NULL COMMENT '会员结束时间',
    `team_id`         BIGINT        NULL DEFAULT NULL COMMENT '团队ID',
    `is_renewal`      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否续费: 0否 1是',
    `refund_status`   TINYINT       NOT NULL DEFAULT 0 COMMENT '退款状态: 0无 1退款中 2已退款',
    `refund_time`     DATETIME      NULL DEFAULT NULL COMMENT '退款时间',
    `data_sign`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '数据签名',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_pay_status` (`pay_status`),
    KEY `idx_trade_no` (`trade_no`),
    KEY `idx_team_id` (`team_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员订单表';

-- -----------------------------------------------------------
-- 9. t_invitation - 邀请记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_invitation`;
CREATE TABLE `t_invitation` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `inviter_id`         BIGINT      NOT NULL DEFAULT 0 COMMENT '邀请人ID',
    `invitee_id`         BIGINT      NOT NULL DEFAULT 0 COMMENT '被邀请人ID',
    `invitee_phone`      VARCHAR(20) NOT NULL DEFAULT '' COMMENT '被邀请人手机号',
    `level`              TINYINT     NOT NULL DEFAULT 0 COMMENT '邀请层级: 1一级 2二级',
    `root_inviter_id`    BIGINT      NOT NULL DEFAULT 0 COMMENT '根邀请人ID',
    `channel`            VARCHAR(30) NOT NULL DEFAULT '' COMMENT '邀请渠道',
    `has_paid`           TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已付费: 0否 1是',
    `first_paid_at`      DATETIME    NULL DEFAULT NULL COMMENT '首次付费时间',
    `device_fingerprint` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备指纹',
    `ip_address`         VARCHAR(45) NOT NULL DEFAULT '' COMMENT 'IP地址',
    `status`             TINYINT     NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1无效',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_inviter_id` (`inviter_id`),
    KEY `idx_invitee_id` (`invitee_id`),
    KEY `idx_root_inviter_id` (`root_inviter_id`),
    KEY `idx_has_paid` (`has_paid`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请记录表';

-- -----------------------------------------------------------
-- 10. t_commission - 返佣记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_commission`;
CREATE TABLE `t_commission` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT        NOT NULL DEFAULT 0 COMMENT '返佣用户ID',
    `invitation_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '邀请记录ID',
    `order_id`        BIGINT        NOT NULL DEFAULT 0 COMMENT '关联订单ID',
    `commission_type` TINYINT       NOT NULL DEFAULT 0 COMMENT '返佣类型: 1一级首购 2一级续费 3二级首购 4二级续费',
    `amount`          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '返佣金额',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0待结算 1已结算 2已撤销',
    `settle_time`     DATETIME      NULL DEFAULT NULL COMMENT '结算时间',
    `data_sign`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '数据签名',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_invitation_id` (`invitation_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='返佣记录表';

-- -----------------------------------------------------------
-- 11. t_coupon_template - 优惠券模板表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_coupon_template`;
CREATE TABLE `t_coupon_template` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`             VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '优惠券名称',
    `coupon_type`      TINYINT       NOT NULL DEFAULT 0 COMMENT '类型: 1新人券 2裂变券 3续费券 4唤回券 5团队券',
    `discount_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `min_pay_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最低消费金额',
    `applicable_plans` VARCHAR(20)   NOT NULL DEFAULT '' COMMENT '适用套餐',
    `valid_days`       INT           NOT NULL DEFAULT 0 COMMENT '有效天数',
    `total_count`      INT           NOT NULL DEFAULT -1 COMMENT '发行总量(-1不限)',
    `issued_count`     INT           NOT NULL DEFAULT 0 COMMENT '已发行数量',
    `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1停用',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_type` (`coupon_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- -----------------------------------------------------------
-- 12. t_user_coupon - 用户优惠券表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_user_coupon`;
CREATE TABLE `t_user_coupon` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT        NOT NULL DEFAULT 0 COMMENT '用户ID',
    `template_id`     BIGINT        NOT NULL DEFAULT 0 COMMENT '优惠券模板ID',
    `coupon_type`     TINYINT       NOT NULL DEFAULT 0 COMMENT '优惠券类型',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `use_status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '使用状态: 0未使用 1已使用 2已过期',
    `order_id`        BIGINT        NULL DEFAULT NULL COMMENT '使用的订单ID',
    `expire_time`     DATETIME      NULL DEFAULT NULL COMMENT '过期时间',
    `used_time`       DATETIME      NULL DEFAULT NULL COMMENT '使用时间',
    `source`          VARCHAR(30)   NOT NULL DEFAULT '' COMMENT '来源',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_use_status` (`use_status`),
    KEY `idx_expire_time` (`expire_time`),
    KEY `idx_user_status` (`user_id`, `use_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- -----------------------------------------------------------
-- 13. t_promoter_level - 推广大使等级表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_promoter_level`;
CREATE TABLE `t_promoter_level` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT        NOT NULL DEFAULT 0 COMMENT '用户ID',
    `level`             TINYINT       NOT NULL DEFAULT 1 COMMENT '推广等级',
    `total_invites`     INT           NOT NULL DEFAULT 0 COMMENT '总邀请人数',
    `total_commission`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计返佣',
    `available_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    `frozen_balance`    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
    `total_withdrawn`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计提现',
    `points`            INT           NOT NULL DEFAULT 0 COMMENT '当前积分',
    `total_points`      INT           NOT NULL DEFAULT 0 COMMENT '累计积分',
    `level1_rate`       DECIMAL(10,2) NOT NULL DEFAULT 19.00 COMMENT '一级返佣比例(%)',
    `level2_rate`       DECIMAL(10,2) NOT NULL DEFAULT 5.00 COMMENT '二级返佣比例(%)',
    `data_sign`         VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '数据签名',
    `sign_version`      INT           NOT NULL DEFAULT 1 COMMENT '签名版本',
    `last_reconcile_at` DATETIME      NULL DEFAULT NULL COMMENT '最后对账时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广大使等级表';

-- -----------------------------------------------------------
-- 14. t_points_log - 积分流水表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_points_log`;
CREATE TABLE `t_points_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '用户ID',
    `points`        INT          NOT NULL DEFAULT 0 COMMENT '积分变动值',
    `action`        VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '动作类型',
    `balance_after` INT          NOT NULL DEFAULT 0 COMMENT '变动后余额',
    `ref_id`        BIGINT       NULL DEFAULT NULL COMMENT '关联ID',
    `remark`        VARCHAR(200) NOT NULL DEFAULT '' COMMENT '备注',
    `prev_hash`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '上一条哈希',
    `chain_hash`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '链式哈希',
    `data_sign`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '数据签名',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表';

-- -----------------------------------------------------------
-- 15. t_withdrawal - 提现记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_withdrawal`;
CREATE TABLE `t_withdrawal` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT        NOT NULL DEFAULT 0 COMMENT '用户ID',
    `amount`        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额',
    `withdraw_type` TINYINT       NOT NULL DEFAULT 0 COMMENT '提现方式: 1微信 2支付宝',
    `account_info`  VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '账户信息',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0申请中 1处理中 2已到账 3已拒绝',
    `reject_reason` VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '拒绝原因',
    `trade_no`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '交易流水号',
    `completed_at`  DATETIME      NULL DEFAULT NULL COMMENT '完成时间',
    `data_sign`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '数据签名',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提现记录表';

-- -----------------------------------------------------------
-- 16. t_team - 团队表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_team`;
CREATE TABLE `t_team` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`               VARCHAR(100) NOT NULL DEFAULT '' COMMENT '团队名称',
    `owner_id`           BIGINT       NOT NULL DEFAULT 0 COMMENT '团队所有者ID',
    `invite_code`        VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '团队邀请码',
    `max_members`        INT          NOT NULL DEFAULT 0 COMMENT '最大成员数',
    `current_members`    INT          NOT NULL DEFAULT 0 COMMENT '当前成员数',
    `member_expire_time` DATETIME     NULL DEFAULT NULL COMMENT '团队会员到期时间',
    `status`             TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1解散',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- -----------------------------------------------------------
-- 17. t_team_member - 团队成员表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_team_member`;
CREATE TABLE `t_team_member` (
    `id`        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `team_id`   BIGINT   NOT NULL DEFAULT 0 COMMENT '团队ID',
    `user_id`   BIGINT   NOT NULL DEFAULT 0 COMMENT '用户ID',
    `role`      TINYINT  NOT NULL DEFAULT 2 COMMENT '角色: 1管理员 2普通成员',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `status`    TINYINT  NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1已退出',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队成员表';

-- -----------------------------------------------------------
-- 18. t_audit_log - 审计日志表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_audit_log`;
CREATE TABLE `t_audit_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `operator_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '操作人ID',
    `operator_type` VARCHAR(10)  NOT NULL DEFAULT '' COMMENT '操作人类型',
    `target_table`  VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '目标表名',
    `target_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '目标记录ID',
    `action`        VARCHAR(10)  NOT NULL DEFAULT '' COMMENT '操作动作',
    `field_name`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '字段名',
    `old_value`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '旧值',
    `new_value`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '新值',
    `ip_address`    VARCHAR(45)  NOT NULL DEFAULT '' COMMENT 'IP地址',
    `user_agent`    VARCHAR(300) NOT NULL DEFAULT '' COMMENT 'User-Agent',
    `request_id`    VARCHAR(36)  NOT NULL DEFAULT '' COMMENT '请求ID',
    `log_sign`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '日志签名',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_target` (`target_table`, `target_id`),
    KEY `idx_action` (`action`),
    KEY `idx_request_id` (`request_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- -----------------------------------------------------------
-- 19. t_share_log - 分享行为记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_share_log`;
CREATE TABLE `t_share_log` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT      NOT NULL DEFAULT 0 COMMENT '用户ID',
    `share_type`     VARCHAR(20) NOT NULL DEFAULT '' COMMENT '分享类型',
    `content_type`   VARCHAR(20) NOT NULL DEFAULT '' COMMENT '内容类型',
    `share_scene`    VARCHAR(30) NOT NULL DEFAULT '' COMMENT '分享场景',
    `click_count`    INT         NOT NULL DEFAULT 0 COMMENT '点击次数',
    `register_count` INT         NOT NULL DEFAULT 0 COMMENT '注册次数',
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_share_type` (`share_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享行为记录表';

-- -----------------------------------------------------------
-- 20. t_analytics_event - 埋点事件表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_analytics_event`;
CREATE TABLE `t_analytics_event` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id`      VARCHAR(36)  NOT NULL DEFAULT '' COMMENT '事件唯一ID',
    `event_type`    VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '事件类型',
    `event_name`    VARCHAR(60)  NOT NULL DEFAULT '' COMMENT '事件名称',
    `user_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '用户ID',
    `session_id`    VARCHAR(36)  NOT NULL DEFAULT '' COMMENT '会话ID',
    `page_path`     VARCHAR(100) NOT NULL DEFAULT '' COMMENT '页面路径',
    `page_title`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '页面标题',
    `referrer_path` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '来源页面路径',
    `platform`      VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '平台',
    `device_model`  VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '设备型号',
    `os`            VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '操作系统',
    `os_version`    VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '系统版本',
    `screen_width`  SMALLINT     NOT NULL DEFAULT 0 COMMENT '屏幕宽度',
    `screen_height` SMALLINT     NOT NULL DEFAULT 0 COMMENT '屏幕高度',
    `network_type`  VARCHAR(10)  NOT NULL DEFAULT '' COMMENT '网络类型',
    `app_version`   VARCHAR(20)  NOT NULL DEFAULT '' COMMENT 'APP版本',
    `member_type`   VARCHAR(10)  NOT NULL DEFAULT '' COMMENT '会员类型',
    `extra`         JSON         NULL COMMENT '扩展数据',
    `event_time`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件发生时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_event_name` (`event_name`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_event_time` (`event_time`),
    KEY `idx_page_path` (`page_path`),
    KEY `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='埋点事件表';

-- -----------------------------------------------------------
-- 21. t_analytics_performance - 页面性能数据表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_analytics_performance`;
CREATE TABLE `t_analytics_performance` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT       NOT NULL DEFAULT 0 COMMENT '用户ID',
    `session_id`       VARCHAR(36)  NOT NULL DEFAULT '' COMMENT '会话ID',
    `page_path`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '页面路径',
    `platform`         VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '平台',
    `fcp`              INT          NOT NULL DEFAULT 0 COMMENT 'First Contentful Paint(ms)',
    `lcp`              INT          NOT NULL DEFAULT 0 COMMENT 'Largest Contentful Paint(ms)',
    `fid`              INT          NOT NULL DEFAULT 0 COMMENT 'First Input Delay(ms)',
    `cls`              DECIMAL(5,3) NOT NULL DEFAULT 0.000 COMMENT 'Cumulative Layout Shift',
    `ttfb`             INT          NOT NULL DEFAULT 0 COMMENT 'Time to First Byte(ms)',
    `load_time`        INT          NOT NULL DEFAULT 0 COMMENT '页面加载时间(ms)',
    `inp`              INT          NOT NULL DEFAULT 0 COMMENT 'Interaction to Next Paint(ms)',
    `dom_ready`        INT          NOT NULL DEFAULT 0 COMMENT 'DOM Ready(ms)',
    `resource_count`   INT          NOT NULL DEFAULT 0 COMMENT '资源数量',
    `resource_size_kb` INT          NOT NULL DEFAULT 0 COMMENT '资源总大小(KB)',
    `js_heap_size_mb`  INT          NOT NULL DEFAULT 0 COMMENT 'JS堆大小(MB)',
    `network_type`     VARCHAR(10)  NOT NULL DEFAULT '' COMMENT '网络类型',
    `event_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件时间',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_page_path` (`page_path`),
    KEY `idx_event_time` (`event_time`),
    KEY `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面性能数据表';

-- -----------------------------------------------------------
-- 22. t_analytics_api - API请求监控表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_analytics_api`;
CREATE TABLE `t_analytics_api` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT       NOT NULL DEFAULT 0 COMMENT '用户ID',
    `session_id`       VARCHAR(36)  NOT NULL DEFAULT '' COMMENT '会话ID',
    `api_path`         VARCHAR(200) NOT NULL DEFAULT '' COMMENT 'API路径',
    `method`           VARCHAR(10)  NOT NULL DEFAULT '' COMMENT '请求方法',
    `status_code`      SMALLINT     NOT NULL DEFAULT 0 COMMENT 'HTTP状态码',
    `duration_ms`      INT          NOT NULL DEFAULT 0 COMMENT '耗时(ms)',
    `request_size_kb`  INT          NOT NULL DEFAULT 0 COMMENT '请求体大小(KB)',
    `response_size_kb` INT          NOT NULL DEFAULT 0 COMMENT '响应体大小(KB)',
    `is_timeout`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否超时: 0否 1是',
    `error_type`       VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '错误类型',
    `page_path`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '来源页面',
    `event_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件时间',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_api_path` (`api_path`),
    KEY `idx_status_code` (`status_code`),
    KEY `idx_event_time` (`event_time`),
    KEY `idx_is_timeout` (`is_timeout`),
    KEY `idx_duration_ms` (`duration_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API请求监控表';

-- -----------------------------------------------------------
-- 23. t_analytics_error - 前端异常表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_analytics_error`;
CREATE TABLE `t_analytics_error` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '用户ID',
    `session_id`          VARCHAR(36)  NOT NULL DEFAULT '' COMMENT '会话ID',
    `error_type`          VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '错误类型',
    `error_message`       VARCHAR(500) NOT NULL DEFAULT '' COMMENT '错误信息',
    `error_stack`         TEXT         NULL COMMENT '错误堆栈',
    `page_path`           VARCHAR(100) NOT NULL DEFAULT '' COMMENT '页面路径',
    `user_action_before`  JSON         NULL COMMENT '错误前用户操作',
    `platform`            VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '平台',
    `device_model`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '设备型号',
    `os`                  VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '操作系统',
    `app_version`         VARCHAR(20)  NOT NULL DEFAULT '' COMMENT 'APP版本',
    `event_time`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件时间',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_error_type` (`error_type`),
    KEY `idx_page_path` (`page_path`),
    KEY `idx_event_time` (`event_time`),
    KEY `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前端异常表';

-- ============================================================
-- 系统初始化数据
-- ============================================================

-- 费用分类预设数据
INSERT INTO `t_expense_category` (`name`, `icon`, `sort_order`, `is_system`, `user_id`, `status`) VALUES
    ('交通费', 'transport', 1, 1, 0, 0),
    ('住宿费', 'hotel',     2, 1, 0, 0),
    ('餐饮费', 'food',      3, 1, 0, 0),
    ('办公费', 'office',    4, 1, 0, 0),
    ('通讯费', 'phone',     5, 1, 0, 0),
    ('其他',   'other',     6, 1, 0, 0);

-- 第一版用户服务协议
INSERT INTO `t_agreement_version` (`agreement_type`, `version_code`, `version_seq`, `title`, `content`, `change_level`, `change_summary`, `change_detail`, `effective_date`, `publish_date`, `status`, `notify_days_before`, `created_by`) VALUES
    (1, 'v1.0', 1, '钱库用户服务协议', '欢迎使用钱库！请仔细阅读以下用户服务协议...', 1, '初始版本', '首次发布用户服务协议', NOW(), NOW(), 2, 15, 0);

-- 第一版隐私政策
INSERT INTO `t_agreement_version` (`agreement_type`, `version_code`, `version_seq`, `title`, `content`, `change_level`, `change_summary`, `change_detail`, `effective_date`, `publish_date`, `status`, `notify_days_before`, `created_by`) VALUES
    (2, 'v1.0', 1, '钱库隐私政策', '钱库非常重视您的隐私保护，请仔细阅读以下隐私政策...', 1, '初始版本', '首次发布隐私政策', NOW(), NOW(), 2, 15, 0);

SET FOREIGN_KEY_CHECKS = 1;
