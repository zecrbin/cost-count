import { Badge, Button, Card, Group, Progress, SimpleGrid, Switch, Text } from '@mantine/core';
import { Plus, Settings2, WalletCards } from 'lucide-react';
import { useMemo, useState } from 'react';
import { AccountAvatar } from '../components/AccountAvatar';
import { AccountDrawer } from '../components/AccountDrawer';
import { AccountModal, InitialBalanceModal } from '../components/AccountModal';
import { EmptyState } from '../components/EmptyState';
import { Money } from '../components/Money';
import { PageHeader } from '../components/PageHeader';
import { useData } from '../lib/data';
import { formatMoney, toNumber } from '../lib/format';
import { ACCOUNT_KINDS, isCredit } from '../lib/meta';
import { navigate } from '../lib/navigation';

function AccountCard({ account, onOpen }) {
  const credit = isCredit(account);
  const limit = toNumber(account.creditLimit);
  const usage = credit && limit > 0 ? (toNumber(account.balance) / limit) * 100 : null;
  return (
    <Card component="button" type="button" className="cc-account-card" onClick={() => onOpen(account.id)}
      data-disabled={account.status === 0 || undefined}>
      <Group gap="sm" wrap="nowrap">
        <AccountAvatar account={account} size={40} />
        <div style={{ minWidth: 0, flex: 1 }}>
          <Group gap={6} wrap="nowrap">
            <Text fw={600} className="cc-truncate">{account.accName}</Text>
            {account.status === 0 && <Badge size="xs" color="gray">停用</Badge>}
          </Group>
          <Text size="xs" c="dimmed" className="cc-truncate">
            {account.providerName} · {account.typeName}{account.accTailNum ? ` · ${account.accTailNum}` : ''}
          </Text>
        </div>
      </Group>
      <Text size="xs" c="dimmed" mt="lg">{credit ? '待还' : '余额'}</Text>
      <Money value={account.balance} fw={650} size="xl" display="block" />
      {usage != null && (
        <>
          <Progress value={Math.min(usage, 100)} size={4} mt="sm" color={usage > 80 ? 'red' : 'indigo'} />
          <Text size="xs" c="dimmed" mt={6}>额度 {formatMoney(limit)} · 可用 {formatMoney(limit - toNumber(account.balance))}</Text>
        </>
      )}
    </Card>
  );
}

function AccountSection({ title, total, accounts, onOpen, onAdd }) {
  return (
    <div style={{ marginTop: 28 }}>
      <Group justify="space-between" mb="sm">
        <Group gap="xs">
          <Text className="cc-section-title">{title}</Text>
          <Text size="sm" c="dimmed">{accounts.length} 个</Text>
        </Group>
        <Money value={total} fw={600} c="dimmed" size="sm" />
      </Group>
      <SimpleGrid cols={{ base: 1, xs: 2, lg: 3 }} spacing="md">
        {accounts.map((account) => <AccountCard key={account.id} account={account} onOpen={onOpen} />)}
        <button type="button" className="cc-add-tile" onClick={onAdd}><Plus size={18} />添加{title}</button>
      </SimpleGrid>
    </div>
  );
}

export function AccountsPage() {
  const { accounts, accountTypes } = useData();
  const [showDisabled, setShowDisabled] = useState(false);
  const [drawerId, setDrawerId] = useState(null);
  const [editor, setEditor] = useState({ opened: false, account: null });
  const [adjusting, setAdjusting] = useState(null);

  const visible = accounts.filter((account) => showDisabled || account.status !== 0);
  const groups = useMemo(() => Object.keys(ACCOUNT_KINDS).map((code) => {
    const items = visible.filter((account) => (account.typeCode || 'DEBIT') === code);
    return { code, items, total: items.reduce((sum, account) => sum + toNumber(account.balance), 0) };
  }), [visible]);
  const assets = groups[0].total;
  const debts = groups[1].total;
  const disabledCount = accounts.filter((account) => account.status === 0).length;

  const openEditor = (account = null) => setEditor({ opened: true, account });

  return (
    <>
      <PageHeader
        title="账户"
        description="资产账户的余额是可用资金，负债账户的余额是待还金额。"
        actions={<Button leftSection={<Plus size={16} />} onClick={() => openEditor()} disabled={!accountTypes.length}>添加账户</Button>}
      />

      {!accountTypes.length ? (
        <Card>
          <EmptyState icon={Settings2} title="先设置机构与账户类型" description="每个账户都属于某个机构的某种类型，例如「招商银行 · 信用卡」。"
            action={<Button onClick={() => navigate('settings')}>去设置</Button>} />
        </Card>
      ) : !accounts.length ? (
        <Card>
          <EmptyState icon={WalletCards} title="还没有账户" description="添加银行卡、支付宝、信用卡等账户，并录入当前余额或欠款。"
            action={<Button leftSection={<Plus size={16} />} onClick={() => openEditor()}>添加账户</Button>} />
        </Card>
      ) : (
        <>
          <Card>
            <Group justify="space-between" align="flex-end" gap="lg">
              <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="xl" style={{ flex: 1 }}>
                <div>
                  <Text className="cc-stat-label">净资产</Text>
                  <Money value={assets - debts} className="cc-stat-value" display="block" mt={4} />
                </div>
                <div>
                  <Text className="cc-stat-label">资产合计</Text>
                  <Money value={assets} className="cc-stat-value" display="block" mt={4} />
                </div>
                <div>
                  <Text className="cc-stat-label">负债合计</Text>
                  <Money value={debts} className="cc-stat-value" display="block" mt={4} />
                </div>
              </SimpleGrid>
              {disabledCount > 0 && (
                <Switch size="sm" label={`显示已停用（${disabledCount}）`} checked={showDisabled}
                  onChange={(event) => setShowDisabled(event.currentTarget.checked)} />
              )}
            </Group>
          </Card>
          {groups.map((group) => (
            <AccountSection key={group.code} title={ACCOUNT_KINDS[group.code].label} total={group.total} accounts={group.items}
              onOpen={setDrawerId} onAdd={() => openEditor()} />
          ))}
        </>
      )}

      <AccountDrawer accountId={drawerId} onClose={() => setDrawerId(null)} onEdit={openEditor} onAdjust={setAdjusting} />
      <AccountModal opened={editor.opened} account={editor.account} onClose={() => setEditor({ opened: false, account: editor.account })} />
      <InitialBalanceModal opened={Boolean(adjusting)} account={adjusting} onClose={() => setAdjusting(null)} />
    </>
  );
}
