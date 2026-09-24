// 金额与日期格式化。后端时间统一为 "YYYY-MM-DD HH:mm:ss"，按本地时间解析。

const moneyFormatter = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

export function toNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
}

/** 格式化金额；sign 为 true 时正数带 "+"，负数始终带 "−"。 */
export function formatMoney(value, { sign = false, symbol = true } = {}) {
  const number = toNumber(value);
  const prefix = number < 0 ? '−' : sign && number > 0 ? '+' : '';
  return `${prefix}${symbol ? '¥' : ''}${moneyFormatter.format(Math.abs(number))}`;
}

/** 图表坐标轴使用的紧凑金额，如 1.2万。 */
export function formatCompactMoney(value) {
  const number = toNumber(value);
  if (Math.abs(number) >= 10000) return `${(number / 10000).toFixed(1).replace(/\.0$/, '')}万`;
  return `${Math.round(number)}`;
}

const pad = (value) => String(value).padStart(2, '0');

export function toApiDateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
    + `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function toApiDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function parseApiDateTime(value) {
  if (!value) return null;
  const [datePart, timePart = '00:00:00'] = String(value).replace('T', ' ').split(' ');
  const [year, month, day] = datePart.split('-').map(Number);
  const [hour = 0, minute = 0, second = 0] = timePart.split(':').map(Number);
  return new Date(year, month - 1, day, hour, minute, second);
}

/** 转换为 <input type="datetime-local"> 的值。 */
export function toInputDateTime(value) {
  const date = value instanceof Date ? value : parseApiDateTime(value);
  if (!date) return '';
  return `${toApiDate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function fromInputDateTime(value) {
  if (!value) return null;
  const [datePart, timePart = '00:00'] = value.split('T');
  return `${datePart} ${timePart.length === 5 ? `${timePart}:00` : timePart}`;
}

export function monthRange({ year, month }) {
  const lastDay = new Date(year, month, 0).getDate();
  return {
    startTime: `${year}-${pad(month)}-01 00:00:00`,
    endTime: `${year}-${pad(month)}-${pad(lastDay)} 23:59:59`,
    days: lastDay,
  };
}

export function currentMonth() {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

export function shiftMonth({ year, month }, delta) {
  const date = new Date(year, month - 1 + delta, 1);
  return { year: date.getFullYear(), month: date.getMonth() + 1 };
}

export function formatMonth({ year, month }) {
  return `${year}年${month}月`;
}

export function isSameMonth(a, b) {
  return a.year === b.year && a.month === b.month;
}

/** 日期分组标题：今天 / 昨天 / 9月23日，附带星期。 */
export function formatDayHeader(dateKey) {
  const date = parseApiDateTime(dateKey);
  const today = new Date();
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  const label = toApiDate(date) === toApiDate(today)
    ? '今天'
    : toApiDate(date) === toApiDate(yesterday)
      ? '昨天'
      : `${date.getMonth() + 1}月${date.getDate()}日`;
  const yearSuffix = date.getFullYear() !== today.getFullYear() ? ` · ${date.getFullYear()}年` : '';
  return { label, weekday: WEEKDAYS[date.getDay()], yearSuffix };
}

export function formatTime(value) {
  return value ? String(value).slice(11, 16) : '';
}

export function formatDate(value) {
  return value ? String(value).slice(0, 10) : '';
}

export function dateKey(value) {
  return String(value).slice(0, 10);
}
