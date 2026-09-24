-- Cost Count 数据库升级脚本
-- 日期：2026-09-04
-- 目的：账户类型采用逻辑删除，并允许删除后重新创建相同编码。
--
-- 注意：MySQL DDL 会自动提交，请在执行前确认当前连接的数据库环境。

-- 1. 删除与逻辑删除冲突的唯一索引。
ALTER TABLE cc_account_type
    DROP INDEX uk_provider_type_code;

-- 2. 保留 provider_id + type_code 查询性能，但不再限制已删除历史记录。
ALTER TABLE cc_account_type
    ADD INDEX idx_provider_type_code (provider_id, type_code);
