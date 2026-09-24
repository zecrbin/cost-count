import {
  ArrowLeftRight, Baby, BookOpen, Briefcase, Bus, Car, Coffee, Coins, CreditCard, Dumbbell, Ellipsis, Film,
  Gamepad2, Gift, GraduationCap, HeartPulse, House, Landmark, Music, PawPrint, PiggyBank, Pill, Plane, Receipt,
  Scale, Shirt, ShoppingBag, ShoppingCart, Smartphone, Sparkles, TrendingUp, Undo2, Users, Utensils, Wifi,
  Wrench, Zap,
} from 'lucide-react';

/** 流水类型：label 为界面名称，tone 决定金额颜色。 */
export const TRANSACTION_TYPES = {
  EXPENSE: { label: '支出', tone: 'expense' },
  INCOME: { label: '收入', tone: 'income' },
  TRANSFER: { label: '转账', tone: 'neutral' },
  ADJUSTMENT: { label: '余额调整', tone: 'neutral' },
  INITIAL: { label: '初始资金', tone: 'neutral' },
};

export const CATEGORY_TYPES = [
  { value: 'EXPENSE', label: '支出' },
  { value: 'INCOME', label: '收入' },
  { value: 'TRANSFER', label: '转账' },
];

export const ACCOUNT_KINDS = {
  DEBIT: { label: '资产账户', short: '储蓄', hint: '余额为可用资金' },
  CREDIT: { label: '负债账户', short: '信用', hint: '余额为待还金额' },
};

/** 分类可选图标，分类表的 icon 字段保存这里的键名。 */
export const CATEGORY_ICONS = {
  Utensils, Coffee, ShoppingBag, ShoppingCart, Shirt, Sparkles, Car, Bus, Plane, House, Zap, Wifi, Smartphone,
  HeartPulse, Pill, Dumbbell, Gamepad2, Film, Music, BookOpen, GraduationCap, Gift, Users, Baby, PawPrint,
  Wrench, Receipt, Landmark, Briefcase, Coins, TrendingUp, PiggyBank, Undo2, ArrowLeftRight, CreditCard, Ellipsis,
};

export const TYPE_FALLBACK_ICONS = {
  EXPENSE: Receipt,
  INCOME: Coins,
  TRANSFER: ArrowLeftRight,
  ADJUSTMENT: Scale,
  INITIAL: PiggyBank,
};

/** 分类和账户没有颜色字段，按名称稳定地映射到一组协调的颜色。 */
export const PALETTE = ['#4263eb', '#0ca678', '#f08c00', '#e64980', '#7048e8', '#1098ad', '#d9480f', '#5c940d', '#ae3ec9', '#1971c2'];

export function colorFromText(text = '') {
  let hash = 0;
  for (const char of String(text)) hash = (hash * 31 + char.codePointAt(0)) >>> 0;
  return PALETTE[hash % PALETTE.length];
}

/** 后端保存的图标相对路径 → 可访问的 URL。 */
export function iconUrl(path) {
  if (!path) return null;
  if (/^(https?:)?\/\//.test(path) || path.startsWith('/') || path.startsWith('data:')) return path;
  // 系统生成的银行卡图标由后端映射在 /icons/accounts，其余为内置静态图标。
  if (path.startsWith('accounts/')) return `/icons/${path}`;
  return `/icons/default/${path.split('/').map(encodeURIComponent).join('/')}`;
}

/** 内置的账户类型图标，文件名为类型名或"机构名+类型名"。 */
const BUILTIN_TYPE_ICONS = new Set(['京东白条', '余额宝', '微信零钱', '微信零钱通', '支付宝余额', '花呗']);

export function accountIconUrl(account) {
  if (!account) return null;
  if (account.icon) return iconUrl(account.icon);
  const candidates = [`${account.providerName || ''}${account.typeName || ''}`, account.typeName];
  const builtin = candidates.find((name) => name && BUILTIN_TYPE_ICONS.has(name));
  if (builtin) return iconUrl(`account-types/${builtin}.png`);
  return iconUrl(account.providerIcon);
}

export function isCredit(accountOrType) {
  return accountOrType?.typeCode === 'CREDIT';
}
