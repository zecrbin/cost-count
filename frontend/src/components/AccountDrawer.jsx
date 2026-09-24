import { AreaChart } from '@mantine/charts';
import {
  Badge, Button, Center, Drawer, Group, Loader, Menu, Progress, SimpleGrid, Stack, Text,
} from '@mantine/core';
import { MoreHorizontal, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { api } from '../api';
import { useData } from '../lib/data';
import { useTransactionEditor } from '../lib/editor';
import { formatCompactMoney, formatDate, formatMoney, toNumber } from '../lib/format';
import { ACCOUNT_KINDS, isCredit } from '../lib/meta';
import { notifyError, notifySuccess } from '../lib/notify';
import { AccountAvatar } from './AccountAvatar';
import { ConfirmAction } from './ConfirmAction';
import { EmptyState } from './EmptyState';
import { Money } from './Money';
import { TransactionList } from './TransactionList';

function InfoItem({ label, children }) {
  return (
    <div>
      <Text size="xs" c="dimmed">{label}</Text>
      <Text size="sm" fw={550} mt={2} component="div">{children}</Text>
    </div>
  );
}

export function AccountDrawer({ accountId, onClose, onEdit, onAdjust }) {
  const { accountMap, ledgerVersion, reload } = useData();
  const { openTransaction } = useTransactionEditor();
  const account = accountId ? accountMap.get(accountId) : null;
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    if (!accountId) return undefined;
    let cancelled = false;
    setDetail(null);
    Promise.all([api.dailyBalances({ accountId }), api.transactions.page({ accountId }, 1, 30)])
      .then(([balances, page]) => {
        if (!cancelled) setDetail({ balances, transactions: page.records, total: page.total });
      })
      .catch((error) => { if (!cancelled) { setDetail({ balances: [], transactions: [], total: 0 }); notifyError(error, '加载账户详情失败'); } });
    return () => { cancelled = true; };
  }, [accountId, ledgerVersion]);

  const remove = async () => {
    try {
      await api.accounts.remove([accountId]);
      notifySuccess('账户已删除');
      await reload('accounts');
      onClose();
    } catch (error) {
      notifyError(error, '删除失败');
    }
  };

  const credit = isCredit(account);
  const limit = toNumber(account?.creditLimit);
  const usage = credit && limit > 0 ? (toNumber(account.balance) / limit) * 100 : null;
  const chartData = (detail?.balances || []).map((item) => ({ date: formatDate(item.statDate).slice(5), 余额: toNumber(item.closingBalance) }));

  return (
    <Drawer opened={Boolean(account)} onClose={onClose} position="right" size="lg" padding="xl"
      title={account && (
        <Group gap="sm" wrap="nowrap">
          <AccountAvatar account={account} size={40} />
          <div>
            <Group gap={6}>
              <Text fw={650}>{account.accName}</Text>
              {account.status === 0 && <Badge color="gray">已停用</Badge>}
            </Group>
            <Text size="xs" c="dimmed">
              {account.providerName} · {account.typeName}{account.accTailNum ? ` · 尾号 ${account.accTailNum}` : ''}
            </Text>
          </div>
        </Group>
      )}>
      {account && (
        <Stack gap="lg">
          <div>
            <Text className="cc-stat-label">{credit ? '当前待还' : '当前余额'}</Text>
            <Money value={account.balance} className="cc-hero-value" display="block" mt={4} />
            {usage != null && (
              <>
                <Progress value={Math.min(usage, 100)} mt="sm" size={6} color={usage > 80 ? 'red' : 'indigo'} />
                <Text size="xs" c="dimmed" mt={6}>已用 {Math.round(usage)}% · 可用 {formatMoney(limit - toNumber(account.balance))}</Text>
              </>
            )}
          </div>

          <Group gap="sm">
            <Button leftSection={<Plus size={16} />} disabled={account.status === 0}
              onClick={() => openTransaction(null, { accountId: account.id })}>
              记一笔
            </Button>
            <Button variant="default" leftSection={<Pencil size={15} />} onClick={() => onEdit(account)}>编辑</Button>
            <Menu position="bottom-end" shadow="md" withinPortal>
              <Menu.Target>
                <Button variant="default" px="sm" aria-label="更多操作"><MoreHorizontal size={16} /></Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item leftSection={<RotateCcw size={15} />} onClick={() => onAdjust(account)}>修正初始资金</Menu.Item>
                <Menu.Item leftSection={<Plus size={15} />} disabled={account.status === 0}
                  onClick={() => openTransaction(null, { accountId: account.id, type: 'ADJUSTMENT' })}>调整当前余额</Menu.Item>
              </Menu.Dropdown>
            </Menu>
            <ConfirmAction message="只有没有交易流水的账户可以删除，有流水的账户请改为停用。确定删除？" onConfirm={remove}>
              <Button variant="subtle" color="red" px="sm" aria-label="删除账户"><Trash2 size={16} /></Button>
            </ConfirmAction>
          </Group>

          <SimpleGrid cols={2} spacing="md" p="md" style={{ background: 'var(--cc-surface-muted)', borderRadius: 12 }}>
            <InfoItem label="账户性质">{ACCOUNT_KINDS[account.typeCode]?.label || '—'}</InfoItem>
            <InfoItem label={credit ? '初始欠款' : '初始余额'}><Money value={account.initialBalance} inherit /></InfoItem>
            <InfoItem label="起始时间">{account.initialTransactionTime?.slice(0, 16) || '—'}</InfoItem>
            {credit ? (
              <InfoItem label="信用额度 / 理想额度">
                {limit ? formatMoney(limit) : '—'} / {account.idealCreditLimit ? formatMoney(account.idealCreditLimit) : '—'}
              </InfoItem>
            ) : (
              <InfoItem label="备注">{account.remark || '—'}</InfoItem>
            )}
          </SimpleGrid>

          <div>
            <Text className="cc-section-title" mb="sm">{credit ? '待还走势' : '余额走势'}</Text>
            {!detail ? (
              <Center h={180}><Loader size="sm" /></Center>
            ) : chartData.length < 2 ? (
              <Text size="sm" c="dimmed">至少两天有流水后显示走势。</Text>
            ) : (
              <AreaChart h={180} data={chartData} dataKey="date" series={[{ name: '余额', color: credit ? 'red.5' : 'indigo.5' }]}
                curveType="monotone" withDots={false} gridAxis="y" tickLine="none" fillOpacity={0.18}
                valueFormatter={(value) => formatMoney(value)} yAxisProps={{ tickFormatter: formatCompactMoney, width: 48 }}
                xAxisProps={{ minTickGap: 24 }} />
            )}
          </div>

          <div>
            <Group justify="space-between" mb="sm">
              <Text className="cc-section-title">最近流水</Text>
              {detail && detail.total > detail.transactions.length && (
                <Text size="xs" c="dimmed">显示最近 {detail.transactions.length} / {detail.total} 笔</Text>
              )}
            </Group>
            <div className="cc-panel" style={{ border: '1px solid var(--cc-border)', borderRadius: 12, overflow: 'hidden' }}>
              {!detail ? (
                <Center h={120}><Loader size="sm" /></Center>
              ) : detail.transactions.length ? (
                <TransactionList transactions={detail.transactions} onSelect={openTransaction} perspectiveAccountId={account.id} showBalance />
              ) : (
                <EmptyState title="还没有流水" py={32} />
              )}
            </div>
          </div>
        </Stack>
      )}
    </Drawer>
  );
}
