const moneyFormat = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** 金额千分位、两位小数；不带货币符号，符号由调用方按语境决定。 */
export function money(value) {
  return moneyFormat.format(Math.abs(Number(value || 0)));
}

/** 带正负号的金额，零不带号。 */
export function signedMoney(value) {
  const n = Number(value || 0);
  if (n === 0) return money(0);
  return `${n > 0 ? '+' : '−'}${money(n)}`;
}

/** 拆出整数和小数部分，大号金额分开排版。 */
export function splitMoney(value) {
  const text = money(value);
  const [integer, fraction] = text.split('.');
  return { integer, fraction, negative: Number(value) < 0 };
}

const pad = (n) => String(n).padStart(2, '0');

/** 本地时间转后端 LocalDateTime 格式：必须带 T，空格分隔会解析失败。 */
export function toLocalDateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/** datetime-local 输入框的值（精确到分钟）。 */
export function toInputDateTime(date) {
  return toLocalDateTime(date).slice(0, 16);
}

/** datetime-local 的值补上秒，交给后端。 */
export function fromInputDateTime(value) {
  return value ? `${value}:00` : null;
}

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];

export function parseDateTime(text) {
  return text ? new Date(text) : null;
}

export function dayKey(text) {
  return text ? text.slice(0, 10) : '';
}

export function describeDay(key) {
  const [y, m, d] = key.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  const today = new Date();
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  const same = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  let relative = WEEKDAYS[date.getDay()];
  if (same(date, today)) relative = `今天 · ${relative}`;
  else if (same(date, yesterday)) relative = `昨天 · ${relative}`;
  return { day: pad(d), month: `${y} 年 ${m} 月`, relative };
}

export function timeOf(text) {
  return text ? text.slice(11, 16) : '';
}

export function todayParts() {
  const now = new Date();
  return {
    day: pad(now.getDate()),
    line: `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 · ${WEEKDAYS[now.getDay()]}`,
  };
}

/** 金额输入：最多两位小数；allowNegative 用于余额调整。 */
export function sanitizeAmount(raw, allowNegative = false) {
  let text = raw.replace(/[^\d.\-]/g, '');
  const negative = allowNegative && text.startsWith('-');
  text = text.replace(/-/g, '');
  const [integer, ...rest] = text.split('.');
  let result = integer;
  if (rest.length) result += `.${rest.join('').slice(0, 2)}`;
  return (negative ? '-' : '') + result;
}

export function isValidAmount(text, allowNegative = false) {
  if (!text || text === '-' || text === '.') return false;
  const n = Number(text);
  if (!Number.isFinite(n)) return false;
  return allowNegative ? n !== 0 : n > 0;
}

export function requestId() {
  if (globalThis.crypto?.randomUUID) return crypto.randomUUID();
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export const isCredit = (typeCode) => String(typeCode || '').toUpperCase() === 'CREDIT';
