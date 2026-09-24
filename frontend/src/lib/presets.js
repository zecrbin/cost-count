// 常用预置数据，用于首次使用时一键导入。图标文件位于后端 static/icons/default。

export const PRESET_CATEGORIES = {
  EXPENSE: [
    ['餐饮', 'Utensils', ['早餐', '午餐', '晚餐', '外卖', '买菜', '零食饮料', '咖啡茶饮']],
    ['交通', 'Car', ['公交地铁', '打车', '加油', '停车', '火车机票']],
    ['购物', 'ShoppingBag', ['日用品', '服饰鞋包', '数码电器', '家居', '美妆护肤']],
    ['居住', 'House', ['房租', '房贷', '水电燃气', '物业费', '维修']],
    ['通讯网络', 'Smartphone', ['话费', '宽带', '会员订阅']],
    ['医疗健康', 'HeartPulse', ['门诊', '药品', '体检', '运动健身']],
    ['娱乐', 'Gamepad2', ['电影演出', '游戏', '旅行']],
    ['学习', 'GraduationCap', ['书籍', '课程', '考试']],
    ['人情往来', 'Gift', ['红包礼金', '礼物', '请客']],
    ['金融费用', 'Landmark', ['手续费', '利息', '保险']],
    ['其他支出', 'Ellipsis', []],
  ],
  INCOME: [
    ['工作收入', 'Briefcase', ['工资', '奖金', '报销', '兼职']],
    ['投资收益', 'TrendingUp', ['理财收益', '利息', '分红']],
    ['其他收入', 'Coins', ['红包', '退款', '二手转卖']],
  ],
  TRANSFER: [
    ['还款', 'CreditCard', []],
    ['理财转入转出', 'PiggyBank', []],
  ],
};

/** 常用机构与账户类型：[机构名, 图标, [[类型名, DEBIT|CREDIT], ...]] */
export const PRESET_PROVIDERS = [
  ['招商银行', 'providers/cmb.png', [['储蓄卡', 'DEBIT'], ['信用卡', 'CREDIT']]],
  ['中国银行', 'providers/boc.png', [['储蓄卡', 'DEBIT'], ['信用卡', 'CREDIT']]],
  ['建设银行', 'providers/ccb.png', [['储蓄卡', 'DEBIT'], ['信用卡', 'CREDIT']]],
  ['农业银行', 'providers/abc.png', [['储蓄卡', 'DEBIT'], ['信用卡', 'CREDIT']]],
  ['支付宝', null, [['余额', 'DEBIT'], ['余额宝', 'DEBIT'], ['花呗', 'CREDIT']]],
  ['微信', null, [['零钱', 'DEBIT'], ['零钱通', 'DEBIT']]],
  ['京东', null, [['白条', 'CREDIT']]],
];
