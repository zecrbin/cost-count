import {
  ActionIcon, Badge, Button, Card, Checkbox, Grid, Group, Modal, NumberInput, SegmentedControl, Stack, Text, TextInput, Tooltip,
} from '@mantine/core';
import { Pencil, Plus, Sparkles, Trash2 } from '../lib/icons';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import { AccountAvatar } from '../components/AccountAvatar';
import { ConfirmModal } from '../components/ConfirmAction';
import { EmptyState } from '../components/EmptyState';
import { PageHeader } from '../components/PageHeader';
import { useData } from '../lib/data';
import { ACCOUNT_KINDS } from '../lib/meta';
import { notifyError, notifySuccess } from '../lib/notify';
import { PRESET_PROVIDERS } from '../lib/presets';

function ProviderModal({ opened, onClose, provider, onSaved }) {
  const { reload } = useData();
  const [name, setName] = useState('');
  const [icon, setIcon] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (opened) { setName(provider?.providerName || ''); setIcon(provider?.icon || ''); }
  }, [opened, provider]);

  const save = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      const payload = { id: provider?.id, providerName: name.trim(), icon: icon.trim() || null };
      const id = await (provider ? api.providers.update(payload) : api.providers.create(payload));
      notifySuccess(provider ? '机构已更新' : '机构已添加');
      await reload('providers', 'accountTypes', 'accounts');
      onSaved?.(id);
      onClose();
    } catch (error) {
      notifyError(error, '保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal opened={opened} onClose={onClose} title={<Text fw={650} size="lg">{provider ? '编辑机构' : '添加机构'}</Text>}>
      <Stack gap="md">
        <TextInput label="机构名称" placeholder="如：招商银行、支付宝" maxLength={128} value={name} data-autofocus
          onChange={(event) => setName(event.currentTarget.value)} />
        <TextInput label="图标路径" placeholder="可选，如 providers/cmb.png" maxLength={256} value={icon}
          description="留空时按名称自动匹配内置图标（招行、中行、建行、农行、支付宝、微信、京东），也可填写图片 URL"
          onChange={(event) => setIcon(event.currentTarget.value)} />
        <Group justify="flex-end" gap="sm">
          <Button variant="default" onClick={onClose}>取消</Button>
          <Button onClick={save} loading={saving} disabled={!name.trim()}>保存</Button>
        </Group>
      </Stack>
    </Modal>
  );
}

function TypeModal({ opened, onClose, provider, accountType }) {
  const { reload, accounts } = useData();
  const [form, setForm] = useState({ typeName: '', typeCode: 'DEBIT', sort: 0 });
  const [saving, setSaving] = useState(false);
  const inUse = accountType && accounts.some((account) => account.typeId === accountType.id);

  useEffect(() => {
    if (opened) setForm({ typeName: accountType?.typeName || '', typeCode: accountType?.typeCode || 'DEBIT', sort: accountType?.sort ?? 0 });
  }, [opened, accountType]);

  const save = async () => {
    if (!form.typeName.trim()) return;
    setSaving(true);
    try {
      const payload = { id: accountType?.id, providerId: provider.id, typeName: form.typeName.trim(), typeCode: form.typeCode, sort: Number(form.sort) || 0 };
      await (accountType ? api.accountTypes.update(payload) : api.accountTypes.create(payload));
      notifySuccess(accountType ? '账户类型已更新' : '账户类型已添加');
      await reload('accountTypes', 'accounts');
      onClose();
    } catch (error) {
      notifyError(error, '保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal opened={opened} onClose={onClose}
      title={<Text fw={650} size="lg">{accountType ? '编辑账户类型' : `为「${provider?.providerName}」添加账户类型`}</Text>}>
      <Stack gap="md">
        <TextInput label="类型名称" placeholder="如：储蓄卡、信用卡、余额宝" maxLength={128} value={form.typeName} data-autofocus
          onChange={(event) => setForm({ ...form, typeName: event.currentTarget.value })} />
        <div>
          <Text size="sm" fw={500} mb={6}>账户性质</Text>
          <SegmentedControl fullWidth value={form.typeCode} disabled={inUse} onChange={(typeCode) => setForm({ ...form, typeCode })}
            data={Object.entries(ACCOUNT_KINDS).map(([value, kind]) => ({ value, label: kind.label }))} />
          <Text size="xs" c="dimmed" mt={6}>
            {inUse ? '已有账户使用该类型，性质不能再修改。' : `${ACCOUNT_KINDS[form.typeCode].hint}。消费会让${form.typeCode === 'CREDIT' ? '待还金额增加' : '余额减少'}。`}
          </Text>
        </div>
        <NumberInput label="排序" min={0} allowDecimal={false} value={form.sort} onChange={(sort) => setForm({ ...form, sort })} />
        <Group justify="flex-end" gap="sm">
          <Button variant="default" onClick={onClose}>取消</Button>
          <Button onClick={save} loading={saving} disabled={!form.typeName.trim()}>保存</Button>
        </Group>
      </Stack>
    </Modal>
  );
}

function PresetModal({ opened, onClose }) {
  const { providers, accountTypes, reload } = useData();
  const [selected, setSelected] = useState([]);
  const [saving, setSaving] = useState(false);
  const existingNames = new Set(providers.map((provider) => provider.providerName));

  useEffect(() => {
    if (opened) setSelected(PRESET_PROVIDERS.map(([name]) => name).filter((name) => !existingNames.has(name)));
  }, [opened]); // eslint-disable-line react-hooks/exhaustive-deps

  const save = async () => {
    setSaving(true);
    try {
      for (const [name, icon, types] of PRESET_PROVIDERS.filter(([presetName]) => selected.includes(presetName))) {
        const existing = providers.find((provider) => provider.providerName === name);
        const providerId = existing?.id || await api.providers.create({ providerName: name, icon });
        const existingTypes = new Set(accountTypes.filter((type) => type.providerId === providerId).map((type) => type.typeName));
        for (const [index, [typeName, typeCode]] of types.entries()) {
          if (!existingTypes.has(typeName)) await api.accountTypes.create({ providerId, typeName, typeCode, sort: index });
        }
      }
      notifySuccess('常用机构已添加');
      onClose();
    } catch (error) {
      notifyError(error, '添加中断');
    } finally {
      await reload('providers', 'accountTypes');
      setSaving(false);
    }
  };

  return (
    <Modal opened={opened} onClose={onClose} title={<Text fw={650} size="lg">添加常用机构</Text>}>
      <Checkbox.Group value={selected} onChange={setSelected}>
        <Stack gap="sm">
          {PRESET_PROVIDERS.map(([name, icon, types]) => (
            <Checkbox key={name} value={name} label={(
              <Group gap="sm" wrap="nowrap">
                <AccountAvatar account={{ providerName: name, providerIcon: icon }} size={26} radius="sm" />
                <div>
                  <Text size="sm" fw={550}>{name}{existingNames.has(name) && <Text span size="xs" c="dimmed"> · 已存在，补全缺少的类型</Text>}</Text>
                  <Text size="xs" c="dimmed">{types.map(([typeName, code]) => `${typeName}（${ACCOUNT_KINDS[code].short}）`).join('、')}</Text>
                </div>
              </Group>
            )} styles={{ body: { alignItems: 'center' } }} />
          ))}
        </Stack>
      </Checkbox.Group>
      <Group justify="flex-end" gap="sm" mt="lg">
        <Button variant="default" onClick={onClose}>取消</Button>
        <Button onClick={save} loading={saving} disabled={!selected.length}>添加 {selected.length} 个机构</Button>
      </Group>
    </Modal>
  );
}

export function SettingsPage() {
  const { providers, accountTypes, accounts, reload } = useData();
  const [selectedId, setSelectedId] = useState(null);
  const [providerEditor, setProviderEditor] = useState({ opened: false, provider: null });
  const [typeEditor, setTypeEditor] = useState({ opened: false, accountType: null });
  const [presetOpened, setPresetOpened] = useState(false);
  const [confirmState, setConfirmState] = useState(null);

  const selected = providers.find((provider) => provider.id === selectedId) || providers[0];
  const types = useMemo(() => accountTypes.filter((type) => type.providerId === selected?.id), [accountTypes, selected]);
  const accountCount = (typeId) => accounts.filter((account) => account.typeId === typeId).length;

  const removeProvider = (provider) => setConfirmState({
    message: `确定删除机构「${provider.providerName}」？需要先删除它下面的账户类型。`,
    onConfirm: async () => {
      try {
        await api.providers.remove([provider.id]);
        notifySuccess('机构已删除');
        setSelectedId(null);
        await reload('providers');
      } catch (error) {
        notifyError(error, '删除失败');
      }
    },
  });

  const removeType = (accountType) => setConfirmState({
    message: `确定删除账户类型「${accountType.typeName}」？已被账户使用的类型无法删除。`,
    onConfirm: async () => {
      try {
        await api.accountTypes.remove([accountType.id]);
        notifySuccess('账户类型已删除');
        await reload('accountTypes');
      } catch (error) {
        notifyError(error, '删除失败');
      }
    },
  });

  return (
    <>
      <PageHeader
        title="机构与类型"
        emoji="🏦"
        description="账户属于某个机构的某种类型，类型决定它是资产还是负债～"
        actions={(
          <>
            <Button variant="default" leftSection={<Sparkles size={16} />} onClick={() => setPresetOpened(true)}>添加常用机构</Button>
            <Button leftSection={<Plus size={16} />} onClick={() => setProviderEditor({ opened: true, provider: null })}>添加机构</Button>
          </>
        )}
      />

      {!providers.length ? (
        <Card>
          <EmptyState mood="calm" title="还没有机构" description="从常用的银行和支付平台开始吧，一键就能加好～"
            action={<Button leftSection={<Sparkles size={16} />} onClick={() => setPresetOpened(true)}>添加常用机构</Button>} />
        </Card>
      ) : (
        <Grid gutter="md">
          <Grid.Col span={{ base: 12, md: 4 }}>
            <Card p="xs">
              <Text size="xs" c="dimmed" fw={600} px="sm" py={6}>机构 · {providers.length}</Text>
              {providers.map((provider) => {
                const count = accountTypes.filter((type) => type.providerId === provider.id).length;
                return (
                  <button key={provider.id} type="button" className="cc-provider-item" data-active={provider.id === selected?.id || undefined}
                    onClick={() => setSelectedId(provider.id)}>
                    <AccountAvatar account={{ providerName: provider.providerName, providerIcon: provider.icon }} size={32} radius="sm" />
                    <Text size="sm" fw={550} style={{ flex: 1 }} className="cc-truncate">{provider.providerName}</Text>
                    <Text size="xs" c="dimmed">{count} 个类型</Text>
                  </button>
                );
              })}
            </Card>
          </Grid.Col>
          <Grid.Col span={{ base: 12, md: 8 }}>
            {selected && (
              <Card p={0}>
                <Group justify="space-between" p="lg" wrap="nowrap">
                  <Group gap="sm" wrap="nowrap">
                    <AccountAvatar account={{ providerName: selected.providerName, providerIcon: selected.icon }} size={40} />
                    <div>
                      <Text fw={650}>{selected.providerName}</Text>
                      <Text size="xs" c="dimmed">{types.length} 个账户类型</Text>
                    </div>
                  </Group>
                  <Group gap={6} wrap="nowrap">
                    <Button size="xs" leftSection={<Plus size={14} />} onClick={() => setTypeEditor({ opened: true, accountType: null })}>添加类型</Button>
                    <Tooltip label="编辑机构">
                      <ActionIcon variant="default" size={30} onClick={() => setProviderEditor({ opened: true, provider: selected })}><Pencil size={15} /></ActionIcon>
                    </Tooltip>
                    <Tooltip label="删除机构">
                      <ActionIcon variant="default" size={30} color="red" onClick={() => removeProvider(selected)}><Trash2 size={15} /></ActionIcon>
                    </Tooltip>
                  </Group>
                </Group>
                <div style={{ borderTop: '1px solid var(--cc-border)' }}>
                  {types.length === 0 ? (
                    <EmptyState title="还没有账户类型" description="例如储蓄卡（资产）、信用卡（负债）。" py={36}
                      action={<Button variant="light" size="xs" onClick={() => setTypeEditor({ opened: true, accountType: null })}>添加类型</Button>} />
                  ) : types.map((type) => (
                    <div className="cc-row" key={type.id}>
                      <div className="cc-row-main">
                        <Group gap={8}>
                          <Text size="sm" fw={550}>{type.typeName}</Text>
                          <Badge size="xs" color={type.typeCode === 'CREDIT' ? 'red' : 'teal'}>{ACCOUNT_KINDS[type.typeCode]?.label || type.typeCode}</Badge>
                        </Group>
                        <Text size="xs" c="dimmed" mt={2}>{accountCount(type.id) ? `${accountCount(type.id)} 个账户在用` : '暂无账户'} · 排序 {type.sort ?? 0}</Text>
                      </div>
                      <Group gap={4} wrap="nowrap">
                        <ActionIcon variant="subtle" color="gray" aria-label="编辑类型" onClick={() => setTypeEditor({ opened: true, accountType: type })}><Pencil size={15} /></ActionIcon>
                        <ActionIcon variant="subtle" color="gray" aria-label="删除类型" onClick={() => removeType(type)}><Trash2 size={15} /></ActionIcon>
                      </Group>
                    </div>
                  ))}
                </div>
              </Card>
            )}
          </Grid.Col>
        </Grid>
      )}

      <ProviderModal opened={providerEditor.opened} provider={providerEditor.provider} onSaved={(id) => !providerEditor.provider && setSelectedId(id)}
        onClose={() => setProviderEditor((current) => ({ ...current, opened: false }))} />
      <TypeModal opened={typeEditor.opened} provider={selected} accountType={typeEditor.accountType}
        onClose={() => setTypeEditor((current) => ({ ...current, opened: false }))} />
      <PresetModal opened={presetOpened} onClose={() => setPresetOpened(false)} />
      <ConfirmModal state={confirmState} onClose={() => setConfirmState(null)} />
    </>
  );
}
