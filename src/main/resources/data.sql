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

INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '生活服务', 'EXPENSE', 'Sparkles', '#4CBF91', TRUE, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM category WHERE type='EXPENSE' AND name='生活服务');
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '学习教育', 'EXPENSE', 'ShoppingBag', '#3977E9', TRUE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM category WHERE type='EXPENSE' AND name='学习教育');
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '金融费用', 'EXPENSE', 'Wallet', '#7C8291', TRUE, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM category WHERE type='EXPENSE' AND name='金融费用');
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '工作收入', 'INCOME', 'Wallet', '#29B37C', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM category WHERE type='INCOME' AND name='工作收入');
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '投资收益', 'INCOME', 'TrendingUp', '#3154E5', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM category WHERE type='INCOME' AND name='投资收益');
INSERT INTO category (name, type, icon, color, system_category, sort, created_time, updated_time) SELECT '其他来源', 'INCOME', 'CirclePlus', '#4CBF91', TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM category WHERE type='INCOME' AND name='其他来源');

UPDATE category SET parent_id=(SELECT id FROM category WHERE type='INCOME' AND name='工作收入') WHERE type='INCOME' AND name IN ('工资','奖金') AND parent_id IS NULL;
UPDATE category SET parent_id=(SELECT id FROM category WHERE type='INCOME' AND name='投资收益') WHERE type='INCOME' AND name='理财收益' AND parent_id IS NULL;
UPDATE category SET parent_id=(SELECT id FROM category WHERE type='INCOME' AND name='其他来源') WHERE type='INCOME' AND name='其他收入' AND parent_id IS NULL;

INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('早餐',1),('午餐',2),('晚餐',3),('外卖',4),('买菜',5),('零食饮料',6),('咖啡茶饮',7)
) v(name,sort) ON p.type='EXPENSE' AND p.name='餐饮' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('公交地铁',1),('打车',2),('加油',3),('停车',4),('高速过路',5),('火车机票',6),('车辆维修',7)
) v(name,sort) ON p.type='EXPENSE' AND p.name='交通' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('日用品',1),('服饰鞋包',2),('数码电器',3),('家具家居',4),('美妆护肤',5)
) v(name,sort) ON p.type='EXPENSE' AND p.name='购物' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('房租',1),('房贷',2),('水费',3),('电费',4),('燃气费',5),('物业费',6),('家居维修',7)
) v(name,sort) ON p.type='EXPENSE' AND p.name='居住' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('挂号问诊',1),('药品',2),('体检',3),('健身运动',4)
) v(name,sort) ON p.type='EXPENSE' AND p.name='医疗' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('电影演出',1),('游戏',2),('会员订阅',3),('旅行度假',4)
) v(name,sort) ON p.type='EXPENSE' AND p.name='娱乐' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('红包礼金',1),('礼物',2),('请客聚餐',3)
) v(name,sort) ON p.type='EXPENSE' AND p.name='人情往来' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('手机通讯',1),('宽带网络',2),('快递物流',3),('理发护理',4),('家政洗衣',5)
) v(name,sort) ON p.type='EXPENSE' AND p.name='生活服务' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('书籍资料',1),('在线课程',2),('考试报名',3),('学费培训',4)
) v(name,sort) ON p.type='EXPENSE' AND p.name='学习教育' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'EXPENSE',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('手续费',1),('贷款利息',2),('保险支出',3)
) v(name,sort) ON p.type='EXPENSE' AND p.name='金融费用' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,'无法归类','EXPENSE',p.icon,p.color,TRUE,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p WHERE p.type='EXPENSE' AND p.name='其他支出' AND NOT EXISTS (SELECT 1 FROM category c WHERE c.type='EXPENSE' AND c.name='无法归类');

INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'INCOME',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('报销',3),('兼职收入',4)
) v(name,sort) ON p.type='INCOME' AND p.name='工作收入' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='INCOME' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'INCOME',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('存款利息',2),('分红',3),('基金股票',4)
) v(name,sort) ON p.type='INCOME' AND p.name='投资收益' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='INCOME' AND c.name=v.name);
INSERT INTO category (parent_id,name,type,icon,color,system_category,sort,created_time,updated_time)
SELECT p.id,v.name,'INCOME',p.icon,p.color,TRUE,v.sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM category p JOIN (VALUES
('退款返还',2),('红包收入',3),('经营收入',4)
) v(name,sort) ON p.type='INCOME' AND p.name='其他来源' WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.type='INCOME' AND c.name=v.name);
