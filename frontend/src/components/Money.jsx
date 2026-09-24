import { Text } from '@mantine/core';
import { formatMoney } from '../lib/format';

/**
 * 金额展示。tone：income 绿色、expense 红色、auto 按正负着色、默认继承文字颜色。
 */
export function Money({ value, tone, sign = false, className = '', ...props }) {
  const number = Number(value || 0);
  const resolvedTone = tone === 'auto' ? (number > 0 ? 'income' : number < 0 ? 'expense' : null) : tone;
  const toneClass = resolvedTone === 'income' ? 'tone-income' : resolvedTone === 'expense' ? 'tone-expense' : '';
  return (
    <Text component="span" className={`num ${toneClass} ${className}`} {...props}>
      {formatMoney(value, { sign })}
    </Text>
  );
}
