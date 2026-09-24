import { BarChart, DonutChart } from '@mantine/charts';
import {
  Button, Card, Center, Grid, Group, Loader, Progress, SimpleGrid, Stack, Text, ThemeIcon,
} from '@mantine/core';
import { ArrowRight, Landmark, PieChart, Plus, ReceiptText, Settings2, WalletCards } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { api, fetchAllTransactions } from '../api';
import { AccountAvatar } from '../components/AccountAvatar';
import { EmptyState } from '../components/EmptyState';
import { Money } from '../components/Money';
import { MonthSwitcher } from '../components/MonthSwitcher';
import { PageHeader } from '../components/PageHeader';
import { TransactionList } from '../components/TransactionList';
import { useData } from '../lib/data';
import { useTransactionEditor } from '../lib/editor';
import { currentMonth, formatCompactMoney, formatMoney, isSameMonth, monthRange, toNumber } from '../lib/format';
import { PALETTE, isCredit } from '../lib/meta';
import { navigate } from '../lib/navigation';
import { notifyError } from '../lib/notify';

function StatCard({ label, value, tone, sign, footnote }) {
  return (
    <Card>
      <Text className="cc-stat-label">{label}</Text>
      <Money value={value} tone={tone} sign={sign} className="cc-stat-value" display="block" mt={6} />
      {footnote && <Text size="xs" c="dimmed" mt={6}>{footnote}</Text>}
    </Card>
  );
}

function Onboarding({ hasTypes }) {
  const steps = [
    { done: hasTypes, icon: Settings2, title: '设置机构与账户类型', text: '例如招商银行的储蓄卡（资产）和信用卡（负债）。', page: 'settings' },
    { done: false, icon: WalletCards, title: '添加账户', text: '填写当前余额或欠款作为初始资金。', page: 'accounts', action: '去添加' },
    { done: false, icon: ReceiptText, title: '开始记账', text: '记录收入、支出和账户间转账，余额自动更新。', page: null },
  ];
  return (
    <Card p="xl">
      <Text fw={650} size="lg">三步开始记账</Text>
      <Text size="sm" c="dimmed" mt={4}>还没有账户。先完成下面的设置，总览就会显示资产、负债和每月收支。</Text>
      <SimpleGrid cols={{ base: 1, sm: 3 }} mt="lg" spacing="md">
        {steps.map((step, index) => (
          <Card key={step.title} bg="var(--cc-surface-muted)" withBorder>
            <Group gap="sm">
              <ThemeIcon variant="light" radius="xl" size={34} color={step.done ? 'teal' : 'indigo'}><step.icon size={17} /></ThemeIcon>
              <Text fw={600} size="sm">{index + 1}. {step.title}</Text>
            </Group>
            <Text size="sm" c="dimmed" mt="sm" mih={40}>{step.text}</Text>
            {step.page && (
              <Button variant={step.done ? 'default' : 'light'} size="xs" mt="md" rightSection={<ArrowRight size={14} />}
                onClick={() => navigate(step.page)}>
                {step.done ? '已完成，去查看' : step.action || '去设置'}
              </Button>
            )}
          </Card>
        ))}
      </SimpleGrid>
    </Card>
  );
}

export function DashboardPage() {
  const { accounts, accountTypes, categoryMap, ledgerVersion } = useData();
  const { openTransaction } = useTransactionEditor();
  const [month, setMonth] = useState(currentMonth);
  const [records, setRecords] = useState(null);
  const [recent, setRecent] = useState([]);

  useEffect(() => {
    let cancelled = false;
    const { startTime, endTime } = monthRange(month);
    setRecords(null);
    Promise.all([fetchAllTransactions({ startTime, endTime }), api.transactions.page({}, 1, 8)])
      .then(([monthRecords, recentPage]) => {
        if (cancelled) return;
        setRecords(monthRecords);
        setRecent(recentPage.records);
      })
      .catch((error) => { if (!cancelled) { setRecords([]); notifyError(error, '加载流水失败'); } });
    return () => { cancelled = true; };
  }, [month, ledgerVersion]);

  const worth = useMemo(() => {
    const assets = accounts.filter((account) => !isCredit(account)).reduce((sum, account) => sum + toNumber(account.balance), 0);
    const debts = accounts.filter(isCredit).reduce((sum, account) => sum + toNumber(account.balance), 0);
    return { assets, debts, net: assets - debts };
  }, [accounts]);

  const stats = useMemo(() => {
    const list = records || [];
    const { days } = monthRange(month);
    const income = list.filter((item) => item.transactionType === 'INCOME').reduce((sum, item) => sum + toNumber(item.amount), 0);
    const expenses = list.filter((item) => item.transactionType === 'EXPENSE');
    const expense = expenses.reduce((sum, item) => sum + toNumber(item.amount), 0);

    const daily = Array.from({ length: days }, (_, index) => ({ day: `${index + 1}`, 支出: 0 }));
    for (const item of expenses) daily[Number(item.transactionTime.slice(8, 10)) - 1].支出 += toNumber(item.amount);

    const byRoot = new Map();
    for (const item of expenses) {
      const root = categoryMap.get(item.categoryId)?.root;
      const name = root?.categoryName || '未分类';
      byRoot.set(name, (byRoot.get(name) || 0) + toNumber(item.amount));
    }
    // 按金额排名分配颜色，保证图中相邻扇区颜色不重复。
    const breakdown = [...byRoot.entries()]
      .map(([name, value]) => ({ name, value: Math.round(value * 100) / 100 }))
      .sort((a, b) => b.value - a.value)
      .map((item, index) => ({ ...item, color: PALETTE[index % PALETTE.length] }));

    const elapsedDays = isSameMonth(month, currentMonth()) ? new Date().getDate() : days;
    return { income, expense, balance: income - expense, daily, breakdown, dailyAverage: expense / elapsedDays, count: list.length };
  }, [records, month, categoryMap]);

  const activeAccounts = accounts.filter((account) => account.status !== 0);

  if (!accounts.length) {
    return (
      <>
        <PageHeader title="总览" description="资产、负债和每月收支一目了然。" />
        <Onboarding hasTypes={accountTypes.length > 0} />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="总览"
        description={`${activeAccounts.length} 个账户 · 本期 ${stats.count} 笔流水`}
        actions={<MonthSwitcher value={month} onChange={setMonth} />}
      />

      <Grid gutter="md">
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card className="cc-hero" h="100%" p="xl">
            <Text className="cc-stat-label">净资产</Text>
            <Money value={worth.net} className="cc-hero-value" display="block" mt={8} />
            <SimpleGrid cols={2} className="cc-hero-split">
              <div>
                <Text className="cc-stat-label" size="xs">资产</Text>
                <Money value={worth.assets} fw={600} size="lg" />
              </div>
              <div>
                <Text className="cc-stat-label" size="xs">负债</Text>
                <Money value={worth.debts} fw={600} size="lg" />
              </div>
            </SimpleGrid>
          </Card>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <SimpleGrid cols={2} spacing="md" h="100%">
            <StatCard label="收入" value={stats.income} tone="income" />
            <StatCard label="支出" value={stats.expense} tone="expense" />
            <StatCard label="结余" value={stats.balance} tone="auto" sign
              footnote={stats.income > 0 ? `结余率 ${Math.round((stats.balance / stats.income) * 100)}%` : '本期暂无收入'} />
            <StatCard label="日均支出" value={stats.dailyAverage} footnote={isSameMonth(month, currentMonth()) ? '按本月已过天数计算' : '按整月天数计算'} />
          </SimpleGrid>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }}>
          <Card h="100%">
            <Text className="cc-section-title">每日支出</Text>
            {records === null ? (
              <Center h={240}><Loader size="sm" /></Center>
            ) : (
              <BarChart h={240} mt="md" data={stats.daily} dataKey="day" series={[{ name: '支出', color: 'indigo.5' }]}
                tickLine="none" gridAxis="y" barProps={{ radius: [4, 4, 0, 0] }} maxBarWidth={14}
                valueFormatter={(value) => formatMoney(value)} yAxisProps={{ tickFormatter: formatCompactMoney, width: 44 }}
                xAxisProps={{ interval: 'preserveStartEnd', minTickGap: 12 }} />
            )}
          </Card>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card h="100%">
            <Text className="cc-section-title">支出构成</Text>
            {records === null ? (
              <Center h={240}><Loader size="sm" /></Center>
            ) : stats.breakdown.length === 0 ? (
              <EmptyState icon={PieChart} title="本期还没有支出" py={56} />
            ) : (
              <Group mt="md" gap="lg" wrap="nowrap" align="center">
                <DonutChart data={stats.breakdown} size={148} thickness={20} withTooltip tooltipDataSource="segment"
                  valueFormatter={(value) => formatMoney(value)} paddingAngle={1} strokeWidth={0} />
                <Stack gap={10} style={{ flex: 1, minWidth: 0 }}>
                  {stats.breakdown.slice(0, 6).map((item) => (
                    <div key={item.name}>
                      <Group justify="space-between" gap="xs" wrap="nowrap">
                        <Group gap={8} wrap="nowrap" style={{ minWidth: 0 }}>
                          <span style={{ width: 8, height: 8, borderRadius: 2, background: item.color, flexShrink: 0 }} />
                          <Text size="sm" className="cc-truncate">{item.name}</Text>
                        </Group>
                        <Text size="sm" fw={550} className="num">{Math.round((item.value / stats.expense) * 100)}%</Text>
                      </Group>
                    </div>
                  ))}
                </Stack>
              </Group>
            )}
          </Card>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card h="100%" p={0}>
            <Group justify="space-between" p="lg" pb="sm">
              <Text className="cc-section-title">账户</Text>
              <Button variant="subtle" size="compact-sm" rightSection={<ArrowRight size={14} />} onClick={() => navigate('accounts')}>全部</Button>
            </Group>
            {activeAccounts.slice(0, 6).map((account) => {
              const credit = isCredit(account);
              const usage = credit && toNumber(account.creditLimit) > 0 ? (toNumber(account.balance) / toNumber(account.creditLimit)) * 100 : null;
              return (
                <div className="cc-row" key={account.id}>
                  <AccountAvatar account={account} size={34} />
                  <div className="cc-row-main">
                    <Text size="sm" fw={550} className="cc-truncate">{account.accName}</Text>
                    {usage != null ? (
                      <Progress value={Math.min(usage, 100)} size={4} mt={6} color={usage > 80 ? 'red' : 'indigo'} />
                    ) : (
                      <Text size="xs" c="dimmed" className="cc-truncate">{account.providerName} · {account.typeName}</Text>
                    )}
                  </div>
                  <div className="cc-row-amount">
                    <Money value={credit ? -toNumber(account.balance) : account.balance} fw={600} size="sm" />
                    {credit && <Text size="xs" c="dimmed">待还</Text>}
                  </div>
                </div>
              );
            })}
          </Card>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Card h="100%" p={0}>
            <Group justify="space-between" p="lg" pb="sm">
              <Text className="cc-section-title">最近流水</Text>
              <Group gap="xs">
                <Button variant="subtle" size="compact-sm" leftSection={<Plus size={14} />} onClick={() => openTransaction()}>记一笔</Button>
                <Button variant="subtle" size="compact-sm" rightSection={<ArrowRight size={14} />} onClick={() => navigate('transactions')}>全部</Button>
              </Group>
            </Group>
            {recent.length ? (
              <TransactionList transactions={recent} onSelect={openTransaction} grouped={false} />
            ) : (
              <EmptyState icon={Landmark} title="还没有流水" description="点击「记一笔」开始记录。" py={40} />
            )}
          </Card>
        </Grid.Col>
      </Grid>
    </>
  );
}
