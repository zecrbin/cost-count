-- 测试用 H2 表结构，与 backend/database/schema.sql 的 MySQL 表结构保持一致（约束和可空性）。
DROP TABLE IF EXISTS cc_account_daily_balance;
DROP TABLE IF EXISTS cc_transaction;
DROP TABLE IF EXISTS cc_category;
DROP TABLE IF EXISTS cc_account;
DROP TABLE IF EXISTS cc_account_type;
DROP TABLE IF EXISTS cc_account_provider;
DROP TABLE IF EXISTS cc_import_record;

CREATE TABLE cc_account_provider (
    id            BIGINT PRIMARY KEY,
    provider_name VARCHAR(128) NOT NULL,
    icon          VARCHAR(256),
    is_deleted    INT          NOT NULL DEFAULT 0,
    created_time  DATETIME,
    created_by    VARCHAR(64),
    updated_time  DATETIME,
    updated_by    VARCHAR(64),
    remark        VARCHAR(256)
);

CREATE TABLE cc_account_type (
    id           BIGINT PRIMARY KEY,
    provider_id  BIGINT       NOT NULL,
    type_code    VARCHAR(64)  NOT NULL,
    type_name    VARCHAR(128) NOT NULL,
    sort         INT          NOT NULL DEFAULT 0,
    is_deleted   INT          NOT NULL DEFAULT 0,
    created_time DATETIME,
    created_by   VARCHAR(64),
    updated_time DATETIME,
    updated_by   VARCHAR(64),
    remark       VARCHAR(256)
);

CREATE TABLE cc_account (
    id                 BIGINT PRIMARY KEY,
    type_id            BIGINT         NOT NULL,
    acc_name           VARCHAR(256)   NOT NULL,
    acc_tail_num       VARCHAR(4),
    balance            DECIMAL(18, 2) NOT NULL DEFAULT 0,
    credit_limit       DECIMAL(18, 2),
    ideal_credit_limit DECIMAL(18, 2),
    icon               VARCHAR(256),
    sort               INT            NOT NULL DEFAULT 0,
    status             INT            NOT NULL DEFAULT 1,
    is_deleted         INT            NOT NULL DEFAULT 0,
    created_time       DATETIME,
    created_by         VARCHAR(64),
    updated_time       DATETIME,
    updated_by         VARCHAR(64),
    remark             VARCHAR(256)
);

CREATE TABLE cc_category (
    id            BIGINT PRIMARY KEY,
    pid           BIGINT               DEFAULT NULL,
    category_type VARCHAR(16) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    icon          VARCHAR(256),
    sort          INT         NOT NULL DEFAULT 0,
    is_deleted    INT         NOT NULL DEFAULT 0,
    created_time  DATETIME,
    created_by    VARCHAR(64),
    updated_time  DATETIME,
    updated_by    VARCHAR(64),
    remark        VARCHAR(256)
);

CREATE TABLE cc_transaction (
    id                BIGINT PRIMARY KEY,
    transaction_type  VARCHAR(64)    NOT NULL,
    category_id       BIGINT,
    account_id        BIGINT         NOT NULL,
    target_account_id BIGINT,
    amount            DECIMAL(18, 2) NOT NULL,
    balance_change    DECIMAL(18, 2) NOT NULL,
    balance_after     DECIMAL(18, 2),
    transaction_time  DATETIME       NOT NULL,
    counterparty      VARCHAR(256),
    source            VARCHAR(16)    NOT NULL,
    request_id        VARCHAR(128),
    is_deleted        INT            NOT NULL DEFAULT 0,
    created_time      DATETIME,
    created_by        VARCHAR(64),
    updated_time      DATETIME,
    updated_by        VARCHAR(64),
    remark            VARCHAR(256),
    CONSTRAINT uk_request_id UNIQUE (request_id)
);

CREATE TABLE cc_account_daily_balance (
    id                 BIGINT PRIMARY KEY,
    account_id         BIGINT         NOT NULL,
    stat_date          DATE           NOT NULL,
    opening_balance    DECIMAL(18, 2) NOT NULL,
    transaction_change DECIMAL(18, 2) NOT NULL,
    correction_change  DECIMAL(18, 2) NOT NULL DEFAULT 0,
    closing_balance    DECIMAL(18, 2) NOT NULL,
    rebuilt_time       DATETIME,
    CONSTRAINT uk_account_stat_date UNIQUE (account_id, stat_date)
);

CREATE TABLE cc_import_record (
    id               BIGINT PRIMARY KEY,
    import_type      VARCHAR(16),
    upload_file_name VARCHAR(256),
    file_url         VARCHAR(512),
    file_hash        VARCHAR(64),
    file_size        BIGINT,
    status           VARCHAR(16),
    total_count      INT,
    success_count    INT,
    duplicate_count  INT,
    error_message    VARCHAR(512),
    import_time      DATETIME,
    is_deleted       INT NOT NULL DEFAULT 0,
    created_time     DATETIME,
    created_by       VARCHAR(64),
    updated_time     DATETIME,
    updated_by       VARCHAR(64),
    remark           VARCHAR(256)
);
