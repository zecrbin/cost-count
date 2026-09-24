import { Button, Card, Center, Group, Loader, SegmentedControl, Select, SimpleGrid, Text, TextInput } from '@mantine/core';
import { Plus, Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { fetchAllTransactions } from '../api';
import { AccountSelect } from '../components/AccountSelect';
import { EmptyState } from '../components/EmptyState';
import { Money } from '../components/Money';
import { MonthSwitcher } from '../components/MonthSwitcher';
import { PageHeader } from '../components/PageHeader';
import { TransactionList } from '../components/TransactionList';
import { useData } from '../lib/data';
import { useTransactionEditor } from '../lib/editor';
import { currentMonth, monthRange, toNumber } from '../lib/format';
import { CATEGORY_TYPES } from '../lib/meta';
import { notifyError } from '../lib/notify';

const TYPE_FILTERS = [
  { value: 'ALL', label: '全部' },
  { value: 'EXPENSE', label: '💸 支出' },
  { value: 'INCOME', label: '💰 收入' },
  { value: 'TRANSFER', label: '🔁 转账' },
  { value: 'OTHER', label: '其他' },
];

function SummaryItem({ label, children }) {
  return (
    <div>
      <Text className="cc-stat-label">{label}</Text>
      <div style={{ marginTop: 4 }}>{children}</div>
    </div>
  );
}

export function TransactionsPage() {
  const { categories, categoryMap, ledgerVersion } = useData();
  const { openTransaction } = useTransactionEditor();
  const [month, setMonth] = useState(currentMonth);
  const [type, setType] = useState('ALL');
  const [accountId, setAccountId] = useState(null);
  const [categoryId, setCategoryId] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [records, setRecords] = useState(null);

  // 按月和账户、类型在服务端过滤；分类（含一级分类下全部明细）和关键词在本地过滤。
  useEffect(() => {
    let cancelled = false;
    const { startTime, endTime } = monthRange(month);
    const transactionType = ['ALL', 'OTHER'].includes(type) ? undefined : type;
    setRecords(null);
    fetchAllTransactions({ startTime, endTime, accountId: accountId || undefined, transactionType })
      .then((result) => { if (!cancelled) setRecords(result); })
      .catch((error) => { if (!cancelled) { setRecords([]); notifyError(error, '加载流水失败'); } });
    return () => { cancelled = true; };
  }, [month, type, accountId, ledgerVersion]);

  const categoryOptions = useMemo(() => CATEGORY_TYPES.map(({ value, label }) => ({
    group: label,
    items: categories.filter((root) => root.categoryType === value).flatMap((root) => [
      { value: root.id, label: root.children?.length ? `${root.categoryName}（全部）` : root.categoryName },
      ...(root.children || []).map((child) => ({ value: child.id, label: `${root.categoryName} / ${child.categoryName}` })),
    ]),
  })).filter((group) => group.items.length), [categories]);

  const filtered = useMemo(() => {
    if (!records) return null;
    const text = keyword.trim().toLowerCase();
    return records.filter((item) => {
      if (type === 'OTHER' && !['ADJUSTMENT', 'INITIAL'].includes(item.transactionType)) return false;
      if (categoryId) {
        const category = categoryMap.get(item.categoryId);
        if (!category || (category.id !== categoryId && category.root?.id !== categoryId)) return false;
      }
      if (text) {
        const haystack = [item.counterparty, item.remark, item.categoryName, item.accountName, item.targetAccountName]
          .filter(Boolean).join(' ').toLowerCase();
        if (!haystack.includes(text)) return false;
      }
      return true;
    });
  }, [records, type, categoryId, keyword, categoryMap]);

  const summary = useMemo(() => {
    const list = filtered || [];
    const sum = (kind) => list.filter((item) => item.transactionType === kind).reduce((total, item) => total + toNumber(item.amount), 0);
    const income = sum('INCOME');
    const expense = sum('EXPENSE');
    return { income, expense, balance: income - expense, count: list.length };
  }, [filtered]);

  const hasFilters = type !== 'ALL' || accountId || categoryId || keyword;

  return (
    <>
      <PageHeader
        title="流水"
        emoji="🧾"
        description="每一笔都在这里，点一下就能修改或删除～"
        actions={<Button visibleFrom="sm" leftSection={<Plus size={16} />} onClick={() => openTransaction()}>记一笔</Button>}
      />

      <Card mb="md">
        <Group justify="space-between" gap="md">
          <MonthSwitcher value={month} onChange={setMonth} />
          <SegmentedControl data={TYPE_FILTERS} value={type} onChange={setType} size="sm" />
        </Group>
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="sm" mt="md">
          <AccountSelect placeholder="全部账户" clearable value={accountId} onChange={setAccountId}
            includeIds={accountId ? [accountId] : []} />
          <Select placeholder="全部分类" clearable searchable data={categoryOptions} value={categoryId} onChange={setCategoryId}
            nothingFoundMessage="没有匹配的分类" />
          <TextInput placeholder="搜索交易对象、备注" leftSection={<Search size={15} />} value={keyword}
            onChange={(event) => setKeyword(event.currentTarget.value)} />
        </SimpleGrid>
        <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="md" mt="lg" pt="md" style={{ borderTop: '1px dashed var(--cc-border)' }}>
          <SummaryItem label="💰 收入"><Money value={summary.income} tone="income" size="xl" /></SummaryItem>
          <SummaryItem label="💸 支出"><Money value={summary.expense} tone="expense" size="xl" /></SummaryItem>
          <SummaryItem label="🐷 结余"><Money value={summary.balance} sign size="xl" /></SummaryItem>
          <SummaryItem label="📝 笔数"><Text size="xl" className="num">{summary.count}</Text></SummaryItem>
        </SimpleGrid>
      </Card>

      <Card p={0} style={{ overflow: 'hidden' }}>
        {filtered === null ? (
          <Center h={240}><Loader size="sm" /></Center>
        ) : filtered.length === 0 ? (
          <EmptyState mood={hasFilters ? 'calm' : 'sleepy'} title={hasFilters ? '没找到符合条件的流水' : '这个月还空空的'}
            description={hasFilters ? '换个筛选条件或切换月份试试？' : '记下第一笔，小猪就醒啦～'}
            action={!hasFilters && <Button variant="light" leftSection={<Plus size={16} />} onClick={() => openTransaction()}>记一笔</Button>} />
        ) : (
          <TransactionList transactions={filtered} onSelect={openTransaction} showBalance={Boolean(accountId)}
            perspectiveAccountId={accountId} />
        )}
      </Card>
    </>
  );
}
