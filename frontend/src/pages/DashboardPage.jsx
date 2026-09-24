import { BarChart, DonutChart } from '../components/Charts';
import { Button, Card, Center, Grid, Group, Loader, Progress, SimpleGrid, Stack, Text } from '@mantine/core';
import { ArrowRight, Plus } from '../lib/icons';
import { useEffect, useMemo, useState } from 'react';
import { api, fetchAllTransactions } from '../api';
import { AccountAvatar } from '../components/AccountAvatar';
import { EmojiBubble } from '../components/CategoryIcon';
import { EmptyState } from '../components/EmptyState';
import { Mascot } from '../components/Mascot';
import { Money } from '../components/Money';
import { MonthSwitcher } from '../components/MonthSwitcher';
import { PageHeader } from '../components/PageHeader';
import { TransactionList } from '../components/TransactionList';
import { useData } from '../lib/data';
import { useTransactionEditor } from '../lib/editor';
import { currentMonth, formatCompactMoney, formatMoney, isSameMonth, monthRange, toNumber } from '../lib/format';
import { PALETTE, categoryEmoji, isCredit } from '../lib/meta';
import { navigate } from '../lib/navigation';
import { notifyError } from '../lib/notify';

function greeting() {
  const hour = new Date().getHours();
  if (hour < 5) return '夜深啦，早点休息 🌙';
  if (hour < 11) return '早上好 ☀️';
  if (hour < 14) return '中午好 🍱';
  if (hour < 18) return '下午好 🌤️';
  return '晚上好 🌙';
}

function StatCard({ label, value, tone, sign, footnote, emoji, color }) {
  return (
    <Card className="cc-stat-card">
      <span className="cc-stat-emoji" style={{ background: `color-mix(in srgb, ${color} 16%, transparent)` }}>{emoji}</span>
      <Text className="cc-stat-label">{label}</Text>
      <Money value={value} tone={tone} sign={sign} className="cc-stat-value" display="block" mt={8} />
      {footnote && <Text size="xs" c="dimmed" fw={600} mt={6}>{footnote}</Text>}
    </Card>
  );
}

function MonthTip({ stats, isCurrent }) {
  let mood = 'calm';
  let text = isCurrent ? '这个月还没有记录哦，先记一笔吧 ✍️' : '这个月没有记录～';
  if (stats.count > 0) {
    if (stats.income > 0 && stats.balance >= 0) {
      mood = 'happy';
      text = `${isCurrent ? '这个月已经' : '这个月'}攒下 ${formatMoney(stats.balance)} 啦，结余率 ${Math.round((stats.balance / stats.income) * 100)}%，继续保持 🎉`;
    } else if (stats.balance < 0) {
      mood = 'sad';
      text = `这个月花得比赚得多 ${formatMoney(-stats.balance)}，${isCurrent ? '接下来省着点花呀' : '下个月加油'} 💪`;
    } else {
      text = `已经记了 ${stats.count} 笔，每一笔都算数 📝`;
    }
  }
  return (
    <div className="cc-tip" style={{ marginBottom: 16 }}>
      <Mascot size={40} mood={mood} />
      <Text fw={700} size="sm">{text}</Text>
    </div>
  );
}

function Onboarding({ hasTypes }) {
  const steps = [
    { done: hasTypes, emoji: '🏦', title: '设置机构和账户类型', text: '比如招商银行的储蓄卡、信用卡，一键就能添加常用的。', page: 'settings', action: '去设置' },
    { done: false, emoji: '👛', title: '添加账户', text: '填上现在的余额或欠款，小猪帮你记住起点。', page: 'accounts', action: '去添加' },
    { done: false, emoji: '✍️', title: '开始记账', text: '收入、支出、转账都能记，余额自动算好。', page: null },
  ];
  return (
    <Card p="xl">
      <Group gap="lg" wrap="nowrap" align="center">
        <Mascot size={110} mood="happy" coin />
        <div>
          <Text fw={900} size="xl">欢迎来到小猪记账本！</Text>
          <Text size="sm" c="dimmed" fw={600} mt={4}>跟着下面三步走，马上就能开始记账啦～</Text>
        </div>
      </Group>
      <SimpleGrid cols={{ base: 1, sm: 3 }} mt="xl" spacing="md">
        {steps.map((step, index) => (
          <Card key={step.title} style={{ background: 'var(--cc-surface-muted)', boxShadow: 'none' }}>
            <Group gap="sm">
              <EmojiBubble emoji={step.done ? '✅' : step.emoji} color={PALETTE[index]} size={42} />
              <Text fw={800}>{index + 1}. {step.title}</Text>
            </Group>
            <Text size="sm" c="dimmed" fw={500} mt="sm" mih={44}>{step.text}</Text>
            {step.page && (
              <Button variant={step.done ? 'default' : 'light'} size="xs" mt="md" rightSection={<ArrowRight size={14} />}
                onClick={() => navigate(step.page)}>
                {step.done ? '已完成，去看看' : step.action}
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
      const key = root?.id || 'none';
      const entry = byRoot.get(key) || { name: root?.categoryName || '未分类', emoji: categoryEmoji(root) || '📦', value: 0 };
      entry.value += toNumber(item.amount);
      byRoot.set(key, entry);
    }
    // 按金额排名分配颜色，保证图中相邻扇区颜色不重复。
    const breakdown = [...byRoot.values()]
      .map((item) => ({ ...item, value: Math.round(item.value * 100) / 100 }))
      .sort((a, b) => b.value - a.value)
      .map((item, index) => ({ ...item, color: PALETTE[index % PALETTE.length] }));

    const elapsedDays = isSameMonth(month, currentMonth()) ? new Date().getDate() : days;
    return { income, expense, balance: income - expense, daily, breakdown, dailyAverage: expense / elapsedDays, count: list.length };
  }, [records, month, categoryMap]);

  const activeAccounts = accounts.filter((account) => account.status !== 0);
  const isCurrent = isSameMonth(month, currentMonth());

  if (!accounts.length) {
    return (
      <>
        <PageHeader title={greeting()} description="小猪已经准备好帮你管钱啦" />
        <Onboarding hasTypes={accountTypes.length > 0} />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={greeting()}
        description={`${activeAccounts.length} 个账户 · ${isCurrent ? '本月' : '这个月'}记了 ${stats.count} 笔`}
        actions={<MonthSwitcher value={month} onChange={setMonth} />}
      />
      {records !== null && <MonthTip stats={stats} isCurrent={isCurrent} />}

      <Grid gutter="md">
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card className="cc-hero" h="100%" p="xl">
            <Text className="cc-stat-label">我的净资产 ✨</Text>
            <Money value={worth.net} className="cc-hero-value" display="block" mt={10} />
            <SimpleGrid cols={2} className="cc-hero-split" maw={300}>
              <div>
                <Text className="cc-stat-label" size="xs">资产</Text>
                <Money value={worth.assets} size="lg" />
              </div>
              <div>
                <Text className="cc-stat-label" size="xs">负债</Text>
                <Money value={worth.debts} size="lg" />
              </div>
            </SimpleGrid>
            <Mascot size={96} mood={worth.net >= 0 ? 'happy' : 'sad'} coin className="cc-hero-mascot" />
          </Card>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <SimpleGrid cols={2} spacing="md" h="100%">
            <StatCard label="收入" value={stats.income} tone="income" emoji="💰" color="#1fb88a" />
            <StatCard label="支出" value={stats.expense} tone="expense" emoji="💸" color="#f0674f" />
            <StatCard label="结余" value={stats.balance} tone="auto" sign emoji="🐷" color="#8b5cf6"
              footnote={stats.income > 0 ? `结余率 ${Math.round((stats.balance / stats.income) * 100)}%` : '暂时还没有收入'} />
            <StatCard label="日均支出" value={stats.dailyAverage} emoji="📅" color="#ffb020"
              footnote={isCurrent ? '按本月已过天数算' : '按整月天数算'} />
          </SimpleGrid>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }}>
          <Card h="100%">
            <Text className="cc-section-title">📊 每日支出</Text>
            {records === null ? (
              <Center h={240}><Loader type="dots" /></Center>
            ) : (
              <BarChart h={240} mt="md" data={stats.daily} dataKey="day" series={[{ name: '支出', color: 'berry.4' }]}
                tickLine="none" gridAxis="y" strokeDasharray="4 6" barProps={{ radius: [8, 8, 8, 8] }} maxBarWidth={12}
                valueFormatter={(value) => formatMoney(value)} yAxisProps={{ tickFormatter: formatCompactMoney, width: 44 }}
                xAxisProps={{ interval: 'preserveStartEnd', minTickGap: 12 }} />
            )}
          </Card>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card h="100%">
            <Text className="cc-section-title">🍩 钱花去哪儿了</Text>
            {records === null ? (
              <Center h={240}><Loader type="dots" /></Center>
            ) : stats.breakdown.length === 0 ? (
              <EmptyState mood="happy" title="还没有支出" description="一分钱都没花，真棒！" py={40} />
            ) : (
              <Group mt="md" gap="lg" wrap="nowrap" align="center">
                <DonutChart data={stats.breakdown} size={150} thickness={24} withTooltip tooltipDataSource="segment"
                  valueFormatter={(value) => formatMoney(value)} paddingAngle={3} strokeWidth={0}
                  chartLabel={`${stats.breakdown.length} 类`} />
                <Stack gap={10} style={{ flex: 1, minWidth: 0 }}>
                  {stats.breakdown.slice(0, 6).map((item) => (
                    <Group key={item.name} justify="space-between" gap="xs" wrap="nowrap">
                      <Group gap={8} wrap="nowrap" style={{ minWidth: 0 }}>
                        <span style={{ width: 10, height: 10, borderRadius: 4, background: item.color, flexShrink: 0 }} />
                        <Text size="sm" fw={600} className="cc-truncate">{item.emoji} {item.name}</Text>
                      </Group>
                      <Text size="sm" className="num">{Math.round((item.value / stats.expense) * 100)}%</Text>
                    </Group>
                  ))}
                </Stack>
              </Group>
            )}
          </Card>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card h="100%" p={0}>
            <Group justify="space-between" p="lg" pb="xs">
              <Text className="cc-section-title">👛 我的账户</Text>
              <Button variant="subtle" size="compact-sm" rightSection={<ArrowRight size={14} />} onClick={() => navigate('accounts')}>全部</Button>
            </Group>
            {activeAccounts.slice(0, 6).map((account) => {
              const credit = isCredit(account);
              const usage = credit && toNumber(account.creditLimit) > 0 ? (toNumber(account.balance) / toNumber(account.creditLimit)) * 100 : null;
              return (
                <div className="cc-row" key={account.id}>
                  <AccountAvatar account={account} size={38} />
                  <div className="cc-row-main">
                    <Text size="sm" fw={700} className="cc-truncate">{account.accName}</Text>
                    {usage != null ? (
                      <Progress value={Math.min(usage, 100)} size={6} mt={6} color={usage > 80 ? 'orange' : 'berry'} />
                    ) : (
                      <Text size="xs" c="dimmed" fw={500} className="cc-truncate">{account.providerName} · {account.typeName}</Text>
                    )}
                  </div>
                  <div className="cc-row-amount">
                    <Money value={account.balance} size="sm" />
                    {credit && <Text size="xs" c="dimmed" fw={600}>待还</Text>}
                  </div>
                </div>
              );
            })}
          </Card>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Card h="100%" p={0}>
            <Group justify="space-between" p="lg" pb="xs">
              <Text className="cc-section-title">🧾 最近流水</Text>
              <Group gap="xs">
                <Button variant="subtle" size="compact-sm" leftSection={<Plus size={14} />} onClick={() => openTransaction()}>记一笔</Button>
                <Button variant="subtle" size="compact-sm" rightSection={<ArrowRight size={14} />} onClick={() => navigate('transactions')}>全部</Button>
              </Group>
            </Group>
            {recent.length ? (
              <TransactionList transactions={recent} onSelect={openTransaction} grouped={false} />
            ) : (
              <EmptyState title="还没有流水" description="点「记一笔」开始记录吧～" py={40} />
            )}
          </Card>
        </Grid.Col>
      </Grid>
    </>
  );
}
