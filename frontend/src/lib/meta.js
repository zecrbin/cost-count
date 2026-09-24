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

/** 分类可选的 emoji，分类表的 icon 字段直接保存 emoji。 */
export const CATEGORY_EMOJIS = [
  '🍚', '🍜', '🍔', '🍱', '🥐', '☕', '🧋', '🍰', '🍎', '🥬', '🍺', '🍿',
  '🚇', '🚕', '🚗', '⛽', '🚲', '✈️', '🚄', '🅿️',
  '🛍️', '🛒', '👗', '👟', '💄', '🧴', '🧻', '📱', '💻', '🎧',
  '🏠', '💡', '💧', '🔥', '🛋️', '🔧', '📞', '🌐', '📺',
  '💊', '🏥', '🦷', '💪', '🎬', '🎮', '🎤', '🎨', '📚', '✏️', '🎓',
  '🎁', '🧧', '❤️', '👶', '🐶', '🐱', '💼', '💰', '📈', '🏦', '🪙', '🧾', '💳', '🔁', '🐷', '✨', '📦',
];

/** 早期版本保存的是图标名，这里映射为 emoji 以兼容旧数据。 */
const LEGACY_ICON_EMOJI = {
  Utensils: '🍜', Coffee: '☕', ShoppingBag: '🛍️', ShoppingCart: '🛒', Shirt: '👗', Sparkles: '✨', Car: '🚗',
  Bus: '🚇', Plane: '✈️', House: '🏠', Zap: '💡', Wifi: '🌐', Smartphone: '📱', HeartPulse: '💊', Pill: '💊',
  Dumbbell: '💪', Gamepad2: '🎮', Film: '🎬', Music: '🎤', BookOpen: '📚', GraduationCap: '🎓', Gift: '🎁',
  Users: '👥', Baby: '👶', PawPrint: '🐾', Wrench: '🔧', Receipt: '🧾', Landmark: '🏦', Briefcase: '💼',
  Coins: '🪙', TrendingUp: '📈', PiggyBank: '🐷', Undo2: '↩️', ArrowLeftRight: '🔁', CreditCard: '💳', Ellipsis: '📦',
};

export const TYPE_EMOJI = {
  EXPENSE: '💸',
  INCOME: '💰',
  TRANSFER: '🔁',
  ADJUSTMENT: '⚖️',
  INITIAL: '🐷',
};

/** 取分类的 emoji：自身 → 上级分类 → 名称首字（由调用方兜底）。 */
export function categoryEmoji(category) {
  for (const icon of [category?.icon, category?.root?.icon]) {
    if (!icon) continue;
    if (LEGACY_ICON_EMOJI[icon]) return LEGACY_ICON_EMOJI[icon];
    if (!/^[A-Za-z0-9]+$/.test(icon)) return icon;
  }
  return null;
}

/** 分类和账户没有颜色字段，按名称稳定地映射到一组协调的颜色。 */
export const PALETTE = ['#8b5cf6', '#4da3ff', '#1fb88a', '#ffb020', '#ff8a5b', '#1fb5c9', '#6c7cf5', '#7cc95b', '#b267e6', '#e0679b'];

export function colorFromText(text = '') {
  let hash = 0;
  for (const char of String(text)) hash = (hash * 31 + char.codePointAt(0)) >>> 0;
  return PALETTE[hash % PALETTE.length];
}

/** 后端保存的图标相对路径 → 可访问的 URL。 */
export function iconUrl(path) {
  if (!path) return null;
  if (/^(https?:)?\/\//.test(path) || path.startsWith('/') || path.startsWith('data:')) return path;
  // 系统生成的银行卡图标和用户上传的图标由后端映射在 /icons 下，其余为内置静态图标。
  if (path.startsWith('accounts/') || path.startsWith('uploads/')) return `/icons/${path}`;
  return `/icons/default/${path.split('/').map(encodeURIComponent).join('/')}`;
}

/**
 * 内置图标（后端 static/icons/default），按名称匹配。文件名统一用英文，
 * 避免中文文件名在不同系统的 JVM 文件编码下无法读取。
 */
const BUILTIN_TYPE_ICONS = {
  支付宝余额: 'alipay-balance', 余额宝: 'yuebao', 花呗: 'huabei',
  微信零钱: 'wechat-change', 零钱: 'wechat-change', 微信零钱通: 'wechat-lct', 零钱通: 'wechat-lct',
  京东白条: 'jd-baitiao', 白条: 'jd-baitiao',
};

const BUILTIN_PROVIDER_ICONS = {
  招商银行: 'cmb', 招行: 'cmb', 中国银行: 'boc', 中行: 'boc', 建设银行: 'ccb', 建行: 'ccb',
  农业银行: 'abc', 农行: 'abc', 支付宝: 'alipay', 微信: 'wechat', 微信支付: 'wechat', 京东: 'jd', 京东金融: 'jd',
};

/** 机构图标：已配置的路径优先，否则按机构名称匹配内置图标。 */
export function providerIconUrl(provider) {
  if (!provider) return null;
  if (provider.icon) return iconUrl(provider.icon);
  const builtin = BUILTIN_PROVIDER_ICONS[provider.providerName?.trim()];
  return builtin ? iconUrl(`providers/${builtin}.png`) : null;
}

/** 账户图标：账户自身 → 内置类型图标（如花呗、余额宝）→ 机构图标。 */
export function accountIconUrl(account) {
  if (!account) return null;
  if (account.icon) return iconUrl(account.icon);
  const providerName = account.providerName?.trim() || '';
  const typeName = account.typeName?.trim() || '';
  const builtinType = BUILTIN_TYPE_ICONS[`${providerName}${typeName}`] || BUILTIN_TYPE_ICONS[typeName];
  if (builtinType) return iconUrl(`account-types/${builtinType}.png`);
  return providerIconUrl({ providerName, icon: account.providerIcon });
}

export function isCredit(accountOrType) {
  return accountOrType?.typeCode === 'CREDIT';
}
