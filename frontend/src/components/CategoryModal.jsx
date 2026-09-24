import { Button, Group, Modal, NumberInput, SegmentedControl, Select, Stack, Text, TextInput } from '@mantine/core';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import { useData } from '../lib/data';
import { CATEGORY_EMOJIS, CATEGORY_TYPES, categoryEmoji } from '../lib/meta';
import { notifyError, notifySuccess } from '../lib/notify';

const ROOT = '0';

/** 新建 / 编辑分类。defaults 可预设 type 与 pid（在某个一级分类下添加明细）。 */
export function CategoryModal({ opened, onClose, category, defaults }) {
  const { categories, reload } = useData();
  const editing = Boolean(category);
  const [form, setForm] = useState({ categoryType: 'EXPENSE', pid: ROOT, categoryName: '', icon: null, sort: 0, remark: '' });
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!opened) return;
    setError(null);
    setForm(category ? {
      categoryType: category.categoryType,
      pid: category.pid && category.pid !== ROOT ? category.pid : ROOT,
      categoryName: category.categoryName,
      icon: categoryEmoji({ icon: category.icon }) || null,
      sort: category.sort ?? 0,
      remark: category.remark || '',
    } : {
      categoryType: defaults?.categoryType || 'EXPENSE', pid: defaults?.pid || ROOT, categoryName: '', icon: null, sort: 0, remark: '',
    });
  }, [opened, category, defaults]);

  const set = (patch) => setForm((current) => ({ ...current, ...patch }));
  const hasChildren = Boolean(category?.children?.length);
  const parentOptions = useMemo(() => [
    { value: ROOT, label: '无（作为一级分类）' },
    ...categories
      .filter((root) => root.categoryType === form.categoryType && root.id !== category?.id)
      .map((root) => ({ value: root.id, label: root.categoryName })),
  ], [categories, form.categoryType, category]);

  const save = async () => {
    if (!form.categoryName.trim()) { setError('请填写分类名称'); return; }
    setSaving(true);
    try {
      const payload = {
        id: category?.id, categoryType: form.categoryType, pid: form.pid === ROOT ? 0 : form.pid,
        categoryName: form.categoryName.trim(), icon: form.icon, sort: Number(form.sort) || 0, remark: form.remark.trim() || null,
      };
      await (editing ? api.categories.update(payload) : api.categories.create(payload));
      notifySuccess(editing ? '分类已更新' : '分类已添加');
      await reload('categories');
      onClose();
    } catch (saveError) {
      notifyError(saveError, '保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal opened={opened} onClose={onClose} title={<Text fw={650} size="lg">{editing ? '编辑分类' : '添加分类'}</Text>}>
      <Stack gap="md">
        <SegmentedControl fullWidth data={CATEGORY_TYPES} value={form.categoryType} disabled={editing && hasChildren}
          onChange={(categoryType) => set({ categoryType, pid: ROOT })} />
        <Select label="上级分类" data={parentOptions} value={form.pid} onChange={(pid) => set({ pid: pid || ROOT })}
          disabled={hasChildren} description={hasChildren ? '已有明细的一级分类不能移动' : '最多两级：一级分类下可以添加明细'} />
        <TextInput label="名称" maxLength={64} value={form.categoryName} error={error} data-autofocus
          onChange={(event) => set({ categoryName: event.currentTarget.value })} />
        <div>
          <Text size="sm" fw={600} mb={6}>选个图标 <Text span size="xs" c="dimmed">明细不选时沿用上级的</Text></Text>
          <div className="cc-emoji-grid">
            {CATEGORY_EMOJIS.map((emoji) => (
              <button key={emoji} type="button" className="cc-emoji-option" data-selected={form.icon === emoji || undefined}
                onClick={() => set({ icon: form.icon === emoji ? null : emoji })} aria-label={emoji}>
                {emoji}
              </button>
            ))}
          </div>
        </div>
        <Group grow>
          <NumberInput label="排序" min={0} allowDecimal={false} value={form.sort} onChange={(sort) => set({ sort })} />
          <TextInput label="备注" placeholder="可选" maxLength={256} value={form.remark}
            onChange={(event) => set({ remark: event.currentTarget.value })} />
        </Group>
        <Group justify="flex-end" gap="sm">
          <Button variant="default" onClick={onClose}>取消</Button>
          <Button onClick={save} loading={saving}>保存</Button>
        </Group>
      </Stack>
    </Modal>
  );
}
