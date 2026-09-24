import {
  Badge, Button, Group, Modal, NumberInput, Select, SimpleGrid, Stack, Switch, Text, TextInput,
} from '@mantine/core';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import { useData } from '../lib/data';
import { fromInputDateTime, toInputDateTime } from '../lib/format';
import { ACCOUNT_KINDS } from '../lib/meta';
import { notifyError, notifySuccess } from '../lib/notify';

function emptyForm() {
  return {
    typeId: null, accName: '', accTailNum: '', initialBalance: '', initialTime: toInputDateTime(new Date()),
    creditLimit: '', idealCreditLimit: '', sort: 0, enabled: true, remark: '',
  };
}

function formFromAccount(account) {
  return {
    typeId: account.typeId, accName: account.accName || '', accTailNum: account.accTailNum || '',
    initialBalance: '', initialTime: '', creditLimit: account.creditLimit ?? '', idealCreditLimit: account.idealCreditLimit ?? '',
    sort: account.sort ?? 0, enabled: account.status !== 0, remark: account.remark || '',
  };
}

/** 新建 / 编辑账户。新建时写入初始资金；编辑不改余额。 */
export function AccountModal({ opened, onClose, account, onSaved }) {
  const { providers, accountTypes, reload } = useData();
  const editing = Boolean(account);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (!opened) return;
    setForm(account ? formFromAccount(account) : emptyForm());
    setErrors({});
  }, [opened, account]);

  const set = (patch) => setForm((current) => ({ ...current, ...patch }));
  const typeMap = useMemo(() => new Map(accountTypes.map((type) => [type.id, type])), [accountTypes]);
  const selectedType = typeMap.get(form.typeId);
  const credit = selectedType?.typeCode === 'CREDIT';

  // 编辑时只能换成余额方向相同的类型，其余选项直接禁用。
  const typeOptions = useMemo(() => providers.map((provider) => ({
    group: provider.providerName,
    items: accountTypes.filter((type) => type.providerId === provider.id).map((type) => ({
      value: type.id,
      label: `${provider.providerName} · ${type.typeName}`,
      disabled: editing && account && type.typeCode !== account.typeCode,
    })),
  })).filter((group) => group.items.length), [providers, accountTypes, editing, account]);

  const chooseType = (typeId) => {
    const type = typeMap.get(typeId);
    const suggested = type ? `${type.providerName || ''}${type.typeName}` : '';
    const previous = selectedType ? `${selectedType.providerName || ''}${selectedType.typeName}` : '';
    set({ typeId, ...(!form.accName || form.accName === previous ? { accName: suggested } : {}) });
  };

  const save = async () => {
    const next = {};
    if (!form.typeId) next.typeId = '请选择账户类型';
    if (!form.accName.trim()) next.accName = '请填写账户名称';
    if (form.accTailNum && !/^\d{4}$/.test(form.accTailNum)) next.accTailNum = '尾号为 4 位数字';
    setErrors(next);
    if (Object.keys(next).length) return;

    const payload = {
      id: account?.id,
      typeId: form.typeId,
      accName: form.accName.trim(),
      accTailNum: form.accTailNum || null,
      creditLimit: credit && form.creditLimit !== '' ? Number(form.creditLimit) : null,
      idealCreditLimit: credit && form.idealCreditLimit !== '' ? Number(form.idealCreditLimit) : null,
      icon: account?.icon && !account.icon.startsWith('accounts/') ? account.icon : null,
      sort: Number(form.sort) || 0,
      status: form.enabled ? 1 : 0,
      remark: form.remark.trim() || null,
      ...(editing ? {} : {
        initialBalance: form.initialBalance === '' ? 0 : Number(form.initialBalance),
        initialTransactionTime: fromInputDateTime(form.initialTime),
      }),
    };
    setSaving(true);
    try {
      const id = await (editing ? api.accounts.update(payload) : api.accounts.create(payload));
      notifySuccess(editing ? '账户已更新' : '账户已添加');
      await reload('accounts');
      onSaved?.(id);
      onClose();
    } catch (error) {
      notifyError(error, '保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal opened={opened} onClose={onClose} size={520} title={<Text fw={650} size="lg">{editing ? '编辑账户' : '添加账户'}</Text>}>
      <Stack gap="md">
        <Select label="账户类型" placeholder={typeOptions.length ? '选择机构和类型' : '请先在「机构与类型」中添加'} data={typeOptions}
          value={form.typeId} onChange={chooseType} searchable error={errors.typeId} nothingFoundMessage="没有匹配的类型"
          rightSection={selectedType ? <Badge size="xs" color={credit ? 'red' : 'teal'}>{ACCOUNT_KINDS[selectedType.typeCode]?.short}</Badge> : undefined}
          rightSectionWidth={selectedType ? 52 : undefined}
          description={selectedType ? ACCOUNT_KINDS[selectedType.typeCode]?.hint : undefined} />
        <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="sm">
          <TextInput label="账户名称" description="显示在账户列表和记账选项中" maxLength={256} value={form.accName} error={errors.accName}
            onChange={(event) => set({ accName: event.currentTarget.value })} />
          <TextInput label="卡号尾号" placeholder="可选，4 位数字" maxLength={4} inputMode="numeric" value={form.accTailNum}
            error={errors.accTailNum} description="填写后自动生成卡面图标"
            onChange={(event) => set({ accTailNum: event.currentTarget.value.replace(/\D/g, '') })} />
        </SimpleGrid>
        {!editing && (
          <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="sm">
            <NumberInput label={credit ? '当前欠款' : '当前余额'} placeholder="0.00" prefix="¥ " min={0} decimalScale={2}
              thousandSeparator="," hideControls value={form.initialBalance} onChange={(initialBalance) => set({ initialBalance })}
              description="作为初始资金入账，之后由流水自动计算" />
            <TextInput label="起始时间" type="datetime-local" value={form.initialTime}
              onChange={(event) => set({ initialTime: event.currentTarget.value })} description="之前的流水无法记入该账户" />
          </SimpleGrid>
        )}
        {credit && (
          <SimpleGrid cols={2} spacing="sm">
            <NumberInput label="信用额度" placeholder="可选" prefix="¥ " min={0} decimalScale={2} thousandSeparator="," hideControls
              value={form.creditLimit} onChange={(creditLimit) => set({ creditLimit })} />
            <NumberInput label="理想额度" placeholder="可选" prefix="¥ " min={0} decimalScale={2} thousandSeparator="," hideControls
              value={form.idealCreditLimit} onChange={(idealCreditLimit) => set({ idealCreditLimit })} />
          </SimpleGrid>
        )}
        <SimpleGrid cols={2} spacing="sm">
          <NumberInput label="排序" min={0} allowDecimal={false} value={form.sort} onChange={(sort) => set({ sort })}
            description="数字越小越靠前" />
          {editing && (
            <Switch label="启用" description="停用后不能再记入新流水" mt={24} checked={form.enabled}
              onChange={(event) => set({ enabled: event.currentTarget.checked })} />
          )}
        </SimpleGrid>
        <TextInput label="备注" placeholder="可选" maxLength={256} value={form.remark}
          onChange={(event) => set({ remark: event.currentTarget.value })} />
        <Group justify="flex-end" gap="sm" mt={4}>
          <Button variant="default" onClick={onClose}>取消</Button>
          <Button onClick={save} loading={saving}>{editing ? '保存' : '添加账户'}</Button>
        </Group>
      </Stack>
    </Modal>
  );
}

/** 修正初始资金：只改初始资金流水的金额，后续余额自动重算。 */
export function InitialBalanceModal({ opened, onClose, account }) {
  const { ledgerChanged } = useData();
  const [amount, setAmount] = useState('');
  const [remark, setRemark] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (opened && account) { setAmount(Number(account.initialBalance ?? 0)); setRemark(''); }
  }, [opened, account]);

  const save = async () => {
    setSaving(true);
    try {
      await api.accounts.adjustInitialBalance(account.id, { initialBalance: Number(amount) || 0, remark: remark.trim() || null });
      notifySuccess('初始资金已修正，余额已重新计算');
      await ledgerChanged();
      onClose();
    } catch (error) {
      notifyError(error, '修正失败');
    } finally {
      setSaving(false);
    }
  };

  const credit = account?.typeCode === 'CREDIT';
  return (
    <Modal opened={opened} onClose={onClose} title={<Text fw={650} size="lg">修正初始资金</Text>}>
      <Stack gap="md">
        <Text size="sm" c="dimmed">
          用于更正开户时录入的{credit ? '欠款' : '余额'}。修改后，该账户此后每一笔流水的余额都会重新计算。
        </Text>
        <NumberInput label={credit ? '初始欠款' : '初始余额'} prefix="¥ " min={0} decimalScale={2} thousandSeparator="," hideControls
          value={amount} onChange={setAmount} data-autofocus />
        <TextInput label="修正说明" placeholder="可选" maxLength={256} value={remark} onChange={(event) => setRemark(event.currentTarget.value)} />
        <Group justify="flex-end" gap="sm">
          <Button variant="default" onClick={onClose}>取消</Button>
          <Button onClick={save} loading={saving}>保存</Button>
        </Group>
      </Stack>
    </Modal>
  );
}
