CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    type VARCHAR(20) NOT NULL,
    nature VARCHAR(12) DEFAULT 'ASSET' NOT NULL,
    balance DECIMAL(18, 2) NOT NULL,
    initial_balance DECIMAL(18, 2) NOT NULL,
    color VARCHAR(20),
    sort INT DEFAULT 0,
    created_time TIMESTAMP,
    updated_time TIMESTAMP
);

ALTER TABLE account ADD COLUMN IF NOT EXISTS nature VARCHAR(12) DEFAULT 'ASSET' NOT NULL;
UPDATE account SET nature = 'LIABILITY' WHERE type = '花呗' OR type LIKE '%信用卡%';

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(20) NOT NULL,
    type VARCHAR(10) NOT NULL,
    icon VARCHAR(30),
    color VARCHAR(20),
    system_category BOOLEAN DEFAULT FALSE,
    sort INT DEFAULT 0,
    created_time TIMESTAMP,
    updated_time TIMESTAMP
);

ALTER TABLE category ADD COLUMN IF NOT EXISTS parent_id BIGINT;

CREATE TABLE IF NOT EXISTS transaction_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(10) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    category_id BIGINT,
    account_id BIGINT NOT NULL,
    target_account_id BIGINT,
    transaction_date DATE NOT NULL,
    merchant VARCHAR(60) NOT NULL,
    note VARCHAR(200),
    source VARCHAR(20) DEFAULT 'MANUAL',
    created_time TIMESTAMP,
    updated_time TIMESTAMP,
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account(id)
);

ALTER TABLE transaction_record ADD COLUMN IF NOT EXISTS target_account_id BIGINT;
ALTER TABLE transaction_record ALTER COLUMN category_id SET NULL;

CREATE INDEX IF NOT EXISTS idx_transaction_date ON transaction_record(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transaction_category ON transaction_record(category_id);
CREATE INDEX IF NOT EXISTS idx_transaction_account ON transaction_record(account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_target_account ON transaction_record(target_account_id);
CREATE INDEX IF NOT EXISTS idx_category_parent ON category(parent_id);
