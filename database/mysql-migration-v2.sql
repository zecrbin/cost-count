USE cost_count;

ALTER TABLE account
    ADD COLUMN nature VARCHAR(12) NOT NULL DEFAULT 'ASSET' COMMENT 'ASSET资产，LIABILITY负债' AFTER type;

UPDATE account SET nature = 'LIABILITY' WHERE type = '花呗' OR type LIKE '%信用卡%';

ALTER TABLE transaction_record
    MODIFY category_id BIGINT NULL COMMENT '分类ID，转账为空',
    ADD COLUMN target_account_id BIGINT NULL COMMENT '转入账户ID，仅转账使用' AFTER account_id,
    ADD KEY idx_transaction_target_account (target_account_id),
    ADD CONSTRAINT fk_transaction_target_account FOREIGN KEY (target_account_id) REFERENCES account (id);
