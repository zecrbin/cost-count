import {
  ActionIcon, Button, Group, Modal, NumberInput, SegmentedControl, SimpleGrid, Stack, Text, TextInput,
} from '@mantine/core';
import { ArrowDownUp, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { api } from '../api';
import { useData } from '../lib/data';
import { formatMoney, fromInputDateTime, toInputDateTime } from '../lib/format';
import { notifyError, notifySuccess } from '../lib/notify';
import { AccountSelect } from './AccountSelect';
import { CategoryPicker } from './CategoryPicker';
import { ConfirmAction } from './ConfirmAction';

const TYPE_OPTIONS = [
  { value: 'EXPENSE', label: '💸 支出', name: '支出' },
  { value: 'INCOME', label: '💰 收入', name: '收入' },
  { value: 'TRANSFER', label: '🔁 转账', name: '转账' },
  { value: 'ADJUSTMENT', label: '⚖️ 调整', name: '余额调整' },
];

const SAVED_EMOJI = { EXPENSE: '💸', INCOME: '🎉', TRANSFER: '🔁', ADJUSTMENT: '⚖️' };

const LAST_ACCOUNT_KEY = 'cc.lastAccountId';

function newRequestId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `req-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function readLastAccount() {
  try { return localStorage.getItem(LAST_ACCOUNT_KEY); } catch { return null; }
}

function rememberAccount(accountId) {
  try { localStorage.setItem(LAST_ACCOUNT_KEY, accountId); } catch { /* 忽略隐私模式等存储不可用的情况 */ }
}

function initialForm(transaction, defaults, accounts) {
  if (transaction) {
    const adjustment = transaction.transactionType === 'ADJUSTMENT';
    const change = Number(transaction.balanceChange || 0);
    return {
      type: transaction.transactionType,
      amount: Number(transaction.amount),
      direction: adjustment && change < 0 ? 'decrease' : 'increase',
      categoryId: transaction.categoryId || null,
      accountId: transaction.accountId,
      targetAccountId: transaction.targetAccountId || null,
      time: toInputDateTime(transaction.transactionTime),
      counterparty: transaction.counterparty || '',
      remark: transaction.remark || '',
    };
  }
  const enabled = accounts.filter((account) => account.status !== 0);
  const last = readLastAccount();
  const accountId = defaults?.accountId
    || (enabled.some((account) => account.id === last) ? last : enabled[0]?.id) || null;
  return {
    type: defaults?.type || 'EXPENSE',
    amount: '',
    direction: 'increase',
    categoryId: null,
    accountId,
    targetAccountId: null,
    time: toInputDateTime(new Date()),
    counterparty: '',
    remark: '',
  };
}

/** 记一笔 / 编辑流水。transaction 为空时新增。 */
export function TransactionModal({ opened, onClose, transaction, defaults }) {
  const { accounts, accountMap, ledgerChanged } = useData();
  const editing = Boolean(transaction);
  const [form, setForm] = useState(() => initialForm(transaction, defaults, accounts));
  const [errors, setErrors] = useState({});
  const [requestId, setRequestId] = useState(newRequestId);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!opened) return;
    setForm(initialForm(transaction, defaults, accounts));
    setErrors({});
    setRequestId(newRequestId());
  }, [opened, transaction, defaults]); // eslint-disable-line react-hooks/exhaustive-deps

  const set = (patch) => setForm((current) => ({ ...current, ...patch }));
  const changeType = (type) => set({ type, categoryId: null, ...(type === 'TRANSFER' ? {} : { targetAccountId: null }) });

  const validate = () => {
    const next = {};
    if (!(Number(form.amount) > 0)) next.amount = '请输入大于 0 的金额';
    if (!form.accountId) next.accountId = '请选择账户';
    if ((form.type === 'EXPENSE' || form.type === 'INCOME') && !form.categoryId) next.categoryId = '请选择分类';
    if (form.type === 'TRANSFER' && !form.targetAccountId) next.targetAccountId = '请选择转入账户';
    if (!form.time) next.time = '请选择时间';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const save = async (keepOpen) => {
    if (!validate()) return;
    const amount = Number(form.amount);
    const payload = {
      id: transaction?.id,
      transactionType: form.type,
      accountId: form.accountId,
      targetAccountId: form.type === 'TRANSFER' ? form.targetAccountId : null,
      categoryId: form.type === 'EXPENSE' || form.type === 'INCOME' ? form.categoryId : null,
      amount: form.type === 'ADJUSTMENT' ? null : amount,
      balanceChange: form.type === 'ADJUSTMENT' ? (form.direction === 'decrease' ? -amount : amount) : null,
      transactionTime: fromInputDateTime(form.time),
      counterparty: form.counterparty.trim() || null,
      remark: form.remark.trim() || null,
      requestId: editing ? undefined : requestId,
    };
    setSaving(true);
    try {
      await (editing ? api.transactions.update(payload) : api.transactions.create(payload));
      rememberAccount(form.accountId);
      notifySuccess(editing ? '改好啦' : `记好啦！${TYPE_OPTIONS.find((item) => item.value === form.type).name} ${formatMoney(amount)}`,
        SAVED_EMOJI[form.type]);
      await ledgerChanged();
      if (keepOpen) {
        set({ amount: '', counterparty: '', remark: '' });
        setRequestId(newRequestId());
      } else {
        onClose();
      }
    } catch (error) {
      notifyError(error, '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    try {
      await api.transactions.remove(transaction.id);
      notifySuccess('这笔已经删掉了', '🗑️');
      await ledgerChanged();
      onClose();
    } catch (error) {
      notifyError(error, '删除失败');
    }
  };

  const account = accountMap.get(form.accountId);
  const adjustPreview = form.type === 'ADJUSTMENT' && !editing && account && Number(form.amount) > 0
    ? Number(account.balance) + (form.direction === 'decrease' ? -1 : 1) * Number(form.amount)
    : null;

  return (
    <Modal opened={opened} onClose={onClose} size={540}
      title={<Text fw={900} size="xl">{editing ? '✏️ 修改这一笔' : '✍️ 记一笔'}</Text>}>
      <form onSubmit={(event) => { event.preventDefault(); save(false); }}>
        <Stack gap="md">
          <SegmentedControl fullWidth size="md" data={TYPE_OPTIONS} value={form.type} onChange={changeType} />

          {form.type === 'ADJUSTMENT' && (
            <SegmentedControl fullWidth size="xs" value={form.direction} onChange={(direction) => set({ direction })}
              data={[{ value: 'increase', label: '余额增加' }, { value: 'decrease', label: '余额减少' }]} />
          )}

          <NumberInput className="cc-amount-input" placeholder="0.00" prefix="¥ " min={0} decimalScale={2} thousandSeparator=","
            hideControls value={form.amount} onChange={(amount) => set({ amount })} error={errors.amount} data-autofocus />

          {(form.type === 'EXPENSE' || form.type === 'INCOME') && (
            <CategoryPicker type={form.type} value={form.categoryId} onChange={(categoryId) => set({ categoryId })}
              error={errors.categoryId} />
          )}

          {form.type === 'TRANSFER' ? (
            <Group align="flex-end" gap="xs" wrap="nowrap">
              <AccountSelect label="转出账户" style={{ flex: 1 }} value={form.accountId} error={errors.accountId}
                includeIds={[transaction?.accountId]} onChange={(accountId) => set({ accountId })} />
              <ActionIcon variant="default" size={36} mb={errors.accountId || errors.targetAccountId ? 22 : 0} aria-label="交换账户"
                onClick={() => set({ accountId: form.targetAccountId, targetAccountId: form.accountId })}>
                <ArrowDownUp size={16} style={{ transform: 'rotate(90deg)' }} />
              </ActionIcon>
              <AccountSelect label="转入账户" style={{ flex: 1 }} value={form.targetAccountId} error={errors.targetAccountId}
                excludeId={form.accountId} includeIds={[transaction?.targetAccountId]} onChange={(targetAccountId) => set({ targetAccountId })} />
            </Group>
          ) : (
            <AccountSelect label="账户" value={form.accountId} error={errors.accountId} includeIds={[transaction?.accountId]}
              onChange={(accountId) => set({ accountId })}
              description={adjustPreview != null ? `调整后余额 ${formatMoney(adjustPreview)}（当前 ${formatMoney(account.balance)}）` : undefined} />
          )}

          <SimpleGrid cols={2} spacing="sm">
            <TextInput label="什么时候" type="datetime-local" value={form.time} error={errors.time}
              onChange={(event) => set({ time: event.currentTarget.value })} />
            <TextInput label={form.type === 'INCOME' ? '谁给的' : '在哪儿花的'} placeholder={form.type === 'TRANSFER' ? '可选' : form.type === 'INCOME' ? '如：公司' : '如：楼下便利店'}
              maxLength={256} value={form.counterparty} onChange={(event) => set({ counterparty: event.currentTarget.value })} />
          </SimpleGrid>
          <TextInput label="备注" placeholder="想说点什么～（可选）" maxLength={256} value={form.remark}
            onChange={(event) => set({ remark: event.currentTarget.value })} />

          <Group justify="space-between" mt={4}>
            {editing ? (
              <ConfirmAction message="删掉后账户余额会自动退回，确定要删除这一笔吗？" onConfirm={remove} position="top-start">
                <Button variant="subtle" color="red" leftSection={<Trash2 size={16} />}>删除</Button>
              </ConfirmAction>
            ) : (
              <Button variant="subtle" onClick={() => save(true)} loading={saving}>保存，再记一笔</Button>
            )}
            <Group gap="sm">
              <Button variant="default" onClick={onClose}>取消</Button>
              <Button type="submit" loading={saving} variant="gradient" gradient={{ from: 'berry.6', to: '#ff8fb8', deg: 135 }}>
                {editing ? '保存修改' : '记好了'}
              </Button>
            </Group>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
