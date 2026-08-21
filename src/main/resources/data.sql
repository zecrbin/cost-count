INSERT INTO account (name, type, balance, initial_balance, color, sort, created_time, updated_time)
SELECT '招商银行', '储蓄卡', 32458.60, 14500.00, '#FF6B5F', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM account);
INSERT INTO account (name, type, balance, initial_balance, color, sort, created_time, updated_time)
SELECT '微信钱包', '电子钱包', 4280.35, 3500.00, '#29B37C', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM account) = 1;
INSERT INTO account (name, type, balance, initial_balance, color, sort, created_time, updated_time)
SELECT '支付宝', '电子钱包', 8162.20, 7200.00, '#3977E9', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM account) = 2;
INSERT INTO account (name, type, balance, initial_balance, color, sort, created_time, updated_time)
SELECT '建设银行', '工资卡', 28640.00, 18000.00, '#262A34', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM account) = 3;

INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time)
SELECT '餐饮', 'EXPENSE', 'Utensils', '#FF7467', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM category);
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '交通', 'EXPENSE', 'Car', '#F3C54F', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 1;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '购物', 'EXPENSE', 'ShoppingBag', '#A47BEA', TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 2;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '居住', 'EXPENSE', 'House', '#3154E5', TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 3;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '医疗', 'EXPENSE', 'HeartPulse', '#EF6484', TRUE, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 4;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '娱乐', 'EXPENSE', 'Gamepad2', '#4CBF91', TRUE, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 5;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '人情往来', 'EXPENSE', 'Gift', '#F39D65', TRUE, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 6;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '其他支出', 'EXPENSE', 'Ellipsis', '#A5A9B2', TRUE, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 7;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '工资', 'INCOME', 'Wallet', '#29B37C', TRUE, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 8;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '奖金', 'INCOME', 'Sparkles', '#F3C54F', TRUE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 9;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '理财收益', 'INCOME', 'TrendingUp', '#3154E5', TRUE, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 10;
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '其他收入', 'INCOME', 'CirclePlus', '#4CBF91', TRUE, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM category) = 11;

INSERT INTO transaction_record (type, amount, category_id, account_id, transaction_date, merchant, note, source, created_time, updated_time)
SELECT 'EXPENSE', 46.80, 1, 2, DATE '2026-08-21', '鲜丰水果', '午间水果', 'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM transaction_record);
INSERT INTO transaction_record (type, amount, category_id, account_id, transaction_date, merchant, note, source, created_time, updated_time) SELECT 'EXPENSE', 4.00, 2, 3, DATE '2026-08-21', '杭州地铁', '通勤', 'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM transaction_record) = 1;
INSERT INTO transaction_record (type, amount, category_id, account_id, transaction_date, merchant, note, source, created_time, updated_time) SELECT 'INCOME', 18500.00, 9, 4, DATE '2026-08-20', '工资收入', '8 月工资', 'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM transaction_record) = 2;
INSERT INTO transaction_record (type, amount, category_id, account_id, transaction_date, merchant, note, source, created_time, updated_time) SELECT 'EXPENSE', 268.40, 1, 1, DATE '2026-08-20', '盒马鲜生', '家庭采购', 'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM transaction_record) = 3;
INSERT INTO transaction_record (type, amount, category_id, account_id, transaction_date, merchant, note, source, created_time, updated_time) SELECT 'EXPENSE', 320.00, 2, 3, DATE '2026-08-19', '中国石化', '加油', 'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE (SELECT COUNT(*) FROM transaction_record) = 4;
