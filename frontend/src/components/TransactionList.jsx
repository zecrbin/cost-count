import { Badge, Group, Text } from '@mantine/core';
import { ArrowRight } from 'lucide-react';
import { useMemo } from 'react';
import { useData } from '../lib/data';
import { dateKey, formatDayHeader, formatTime } from '../lib/format';
import { TRANSACTION_TYPES } from '../lib/meta';
import { CategoryIcon } from './CategoryIcon';
import { Money } from './Money';

/**
 * 流水展示：金额以"查看的账户"为视角。
 * 未指定视角账户时：收入为正、支出为负，转账与调整显示业务金额。
 */
export function transactionDisplay(transaction, { categoryMap, perspectiveAccountId } = {}) {
  const type = transaction.transactionType;
  const category = transaction.categoryId ? categoryMap?.get(transaction.categoryId) : null;
  let amount = Number(transaction.amount || 0);
  let tone = TRANSACTION_TYPES[type]?.tone || 'neutral';
  let sign = false;

  // 列表中支出较多，用正文色避免满屏红色，只有收入着色。
  if (type === 'EXPENSE') { amount = -amount; sign = true; tone = 'neutral'; }
  if (type === 'INCOME') { sign = true; }
  if (type === 'ADJUSTMENT') { amount = Number(transaction.balanceChange || 0); sign = true; tone = 'neutral'; }
  if (perspectiveAccountId && type === 'TRANSFER') {
    amount = transaction.accountId === perspectiveAccountId ? -amount : amount;
    sign = true;
  }

  const typeLabel = TRANSACTION_TYPES[type]?.label || type;
  const categoryLabel = category ? (category.parent ? `${category.parent.categoryName}/${category.categoryName}` : category.categoryName) : null;
  const title = transaction.counterparty
    || (type === 'TRANSFER' ? '账户转账' : category?.categoryName || typeLabel);
  return { amount, tone, sign, category, categoryLabel, typeLabel, title };
}

function TransactionRow({ transaction, onSelect, perspectiveAccountId, showBalance, showDate }) {
  const { categoryMap } = useData();
  const display = transactionDisplay(transaction, { categoryMap, perspectiveAccountId });
  const type = transaction.transactionType;
  const editable = type !== 'INITIAL' && onSelect;
  const Component = editable ? 'button' : 'div';

  return (
    <Component className="cc-row" type={editable ? 'button' : undefined} onClick={editable ? () => onSelect(transaction) : undefined}>
      <CategoryIcon category={display.category} type={type} />
      <div className="cc-row-main">
        <Group gap={6} wrap="nowrap">
          <Text fw={550} size="sm" className="cc-truncate">{display.title}</Text>
          {type !== 'EXPENSE' && type !== 'INCOME' && <Badge size="xs" color="gray">{display.typeLabel}</Badge>}
        </Group>
        <Group gap={6} mt={3} wrap="nowrap" c="dimmed" style={{ fontSize: 12 }}>
          <span className="num" style={{ flexShrink: 0 }}>
            {showDate ? String(transaction.transactionTime).slice(5, 16) : formatTime(transaction.transactionTime)}
          </span>
          {display.categoryLabel && type !== 'TRANSFER' && <><span>·</span><span className="cc-truncate">{display.categoryLabel}</span></>}
          <span>·</span>
          {type === 'TRANSFER' ? (
            <Group gap={4} wrap="nowrap" className="cc-truncate">
              <span className="cc-truncate">{transaction.accountName}</span>
              <ArrowRight size={12} style={{ flexShrink: 0 }} />
              <span className="cc-truncate">{transaction.targetAccountName}</span>
            </Group>
          ) : (
            <span className="cc-truncate">{transaction.accountName}</span>
          )}
          {transaction.remark && <><span>·</span><span className="cc-truncate">{transaction.remark}</span></>}
        </Group>
      </div>
      <div className="cc-row-amount">
        <Money value={display.amount} sign={display.sign} tone={display.tone} fw={600} size="sm" />
        {showBalance && transaction.balanceAfter != null && (!perspectiveAccountId || transaction.accountId === perspectiveAccountId) && (
          <Text size="xs" c="dimmed" className="num" mt={2}>余额 <Money value={transaction.balanceAfter} inherit /></Text>
        )}
      </div>
    </Component>
  );
}

/** 按日期分组的流水列表，每组显示当日收支小计。 */
export function TransactionList({ transactions, onSelect, perspectiveAccountId, showBalance = false, grouped = true }) {
  const groups = useMemo(() => {
    const map = new Map();
    for (const transaction of transactions) {
      const key = dateKey(transaction.transactionTime);
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(transaction);
    }
    return [...map.entries()];
  }, [transactions]);

  if (!grouped) {
    return transactions.map((transaction) => (
      <TransactionRow key={transaction.id} transaction={transaction} onSelect={onSelect}
        perspectiveAccountId={perspectiveAccountId} showBalance={showBalance} showDate />
    ));
  }

  return groups.map(([key, items]) => {
    const header = formatDayHeader(key);
    const income = items.filter((item) => item.transactionType === 'INCOME').reduce((sum, item) => sum + Number(item.amount), 0);
    const expense = items.filter((item) => item.transactionType === 'EXPENSE').reduce((sum, item) => sum + Number(item.amount), 0);
    return (
      <div className="cc-day" key={key}>
        <div className="cc-day-header">
          <Group gap={8}>
            <Text fw={600} size="sm">{header.label}</Text>
            <Text size="xs" c="dimmed">{header.weekday}{header.yearSuffix}</Text>
          </Group>
          <Group gap="md">
            {expense > 0 && <Text size="xs" c="dimmed">支出 <Money value={expense} inherit /></Text>}
            {income > 0 && <Text size="xs" c="dimmed">收入 <Money value={income} inherit /></Text>}
          </Group>
        </div>
        {items.map((transaction) => (
          <TransactionRow key={transaction.id} transaction={transaction} onSelect={onSelect}
            perspectiveAccountId={perspectiveAccountId} showBalance={showBalance} />
        ))}
      </div>
    );
  });
}
