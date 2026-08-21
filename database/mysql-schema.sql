CREATE DATABASE IF NOT EXISTS cost_count DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cost_count;

CREATE TABLE IF NOT EXISTS account (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    name VARCHAR(30) NOT NULL COMMENT '账户名称',
    type VARCHAR(20) NOT NULL COMMENT '账户类型',
    balance DECIMAL(18, 2) NOT NULL COMMENT '当前余额',
    initial_balance DECIMAL(18, 2) NOT NULL COMMENT '初始金额',
    color VARCHAR(20) DEFAULT NULL COMMENT '标识颜色',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_time DATETIME DEFAULT NULL,
    updated_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金账户';

CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(20) NOT NULL COMMENT '分类名称',
    type VARCHAR(10) NOT NULL COMMENT 'INCOME收入，EXPENSE支出',
    icon VARCHAR(30) DEFAULT NULL COMMENT '前端图标名称',
    color VARCHAR(20) DEFAULT NULL COMMENT '标识颜色',
    system_category TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统分类',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_time DATETIME DEFAULT NULL,
    updated_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_category_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支分类';

CREATE TABLE IF NOT EXISTS transaction_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '收支记录ID',
    type VARCHAR(10) NOT NULL COMMENT 'INCOME收入，EXPENSE支出',
    amount DECIMAL(18, 2) NOT NULL COMMENT '金额',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    account_id BIGINT NOT NULL COMMENT '账户ID',
    transaction_date DATE NOT NULL COMMENT '交易日期',
    merchant VARCHAR(60) NOT NULL COMMENT '交易对象或收入来源',
    note VARCHAR(200) DEFAULT NULL COMMENT '备注',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '记录来源',
    created_time DATETIME DEFAULT NULL,
    updated_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_transaction_date (transaction_date),
    KEY idx_transaction_category (category_id),
    KEY idx_transaction_account (account_id),
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日常收支记录';
