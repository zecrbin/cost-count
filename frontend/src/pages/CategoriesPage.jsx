import { ActionIcon, Button, Card, Group, Menu, SegmentedControl, SimpleGrid, Text } from '@mantine/core';
import { MoreHorizontal, Pencil, Plus, Sparkles, Trash2 } from '../lib/icons';
import { useState } from 'react';
import { api } from '../api';
import { CategoryIcon } from '../components/CategoryIcon';
import { CategoryModal } from '../components/CategoryModal';
import { ConfirmModal } from '../components/ConfirmAction';
import { EmptyState } from '../components/EmptyState';
import { PageHeader } from '../components/PageHeader';
import { useData } from '../lib/data';
import { CATEGORY_TYPES, categoryEmoji } from '../lib/meta';
import { notifyError, notifySuccess } from '../lib/notify';
import { PRESET_CATEGORIES } from '../lib/presets';

function RootCard({ root, onEdit, onAddChild, onDelete }) {
  const { categoryMap } = useData();
  const children = root.children || [];
  return (
    <Card>
      <Group justify="space-between" wrap="nowrap">
        <Group gap="sm" wrap="nowrap">
          <CategoryIcon category={categoryMap.get(root.id)} size={48} />
          <div>
            <Text fw={800} size="lg">{root.categoryName}</Text>
            <Text size="xs" c="dimmed">{children.length ? `${children.length} 个明细` : '无明细，可直接记账'}</Text>
          </div>
        </Group>
        <Menu position="bottom-end" shadow="md">
          <Menu.Target>
            <ActionIcon variant="subtle" color="gray" aria-label="更多操作"><MoreHorizontal size={18} /></ActionIcon>
          </Menu.Target>
          <Menu.Dropdown>
            <Menu.Item leftSection={<Pencil size={15} />} onClick={() => onEdit(root)}>编辑</Menu.Item>
            <Menu.Item leftSection={<Plus size={15} />} onClick={() => onAddChild(root)}>添加明细</Menu.Item>
            <Menu.Divider />
            <Menu.Item color="red" leftSection={<Trash2 size={15} />} onClick={() => onDelete(root)}>删除</Menu.Item>
          </Menu.Dropdown>
        </Menu>
      </Group>
      <Group gap={8} mt="md">
        {children.map((child) => (
          <button key={child.id} type="button" className="cc-chip" onClick={() => onEdit(child)}>
            {child.icon && <span>{categoryEmoji(child)}</span>}{child.categoryName}
          </button>
        ))}
        <button type="button" className="cc-chip" data-dashed onClick={() => onAddChild(root)}>
          <Plus size={13} />明细
        </button>
      </Group>
    </Card>
  );
}

export function CategoriesPage() {
  const { categories, reload } = useData();
  const [type, setType] = useState('EXPENSE');
  const [editor, setEditor] = useState({ opened: false, category: null, defaults: null });
  const [importing, setImporting] = useState(false);
  const [confirmState, setConfirmState] = useState(null);

  const roots = categories.filter((root) => root.categoryType === type);
  const counts = Object.fromEntries(CATEGORY_TYPES.map(({ value }) => [value, categories.filter((root) => root.categoryType === value).length]));

  const open = (category = null, defaults = null) => setEditor({ opened: true, category, defaults });

  const remove = (root) => {
    const ids = [root.id, ...(root.children || []).map((child) => child.id)];
    const target = root.children?.length ? `「${root.categoryName}」及其 ${root.children.length} 个明细` : `「${root.categoryName}」`;
    setConfirmState({
      message: `确定删除${target}？已被流水使用的分类无法删除。`,
      onConfirm: async () => {
        try {
          await api.categories.remove(ids);
          notifySuccess('分类已删除');
          await reload('categories');
        } catch (error) {
          notifyError(error, '删除失败');
        }
      },
    });
  };

  // 逐个创建预置分类；同名分类已存在时跳过，因此可以重复执行。
  const importPresets = async () => {
    setImporting(true);
    let created = 0;
    try {
      const existing = new Map(roots.map((root) => [root.categoryName, root]));
      for (const [index, [name, icon, children]] of PRESET_CATEGORIES[type].entries()) {
        let rootId = existing.get(name)?.id;
        if (!rootId) {
          rootId = await api.categories.create({ categoryType: type, pid: 0, categoryName: name, icon, sort: index });
          created += 1;
        }
        const existingChildren = new Set((existing.get(name)?.children || []).map((child) => child.categoryName));
        for (const [childIndex, [childName, childIcon]] of children.entries()) {
          if (existingChildren.has(childName)) continue;
          await api.categories.create({ categoryType: type, pid: rootId, categoryName: childName, icon: childIcon, sort: childIndex });
          created += 1;
        }
      }
      notifySuccess(created ? `已添加 ${created} 个分类` : '常用分类已全部存在');
    } catch (error) {
      notifyError(error, '导入中断');
    } finally {
      await reload('categories');
      setImporting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="分类"
        emoji="🏷️"
        description="大类用来看钱花去哪儿了，明细让每一笔记得更准～"
        actions={<Button leftSection={<Plus size={16} />} onClick={() => open(null, { categoryType: type })}>添加分类</Button>}
      />
      <Group justify="space-between" mb="md">
        <SegmentedControl value={type} onChange={setType}
          data={CATEGORY_TYPES.map(({ value, label }) => ({ value, label: `${label} ${counts[value] || ''}`.trim() }))} />
        {roots.length > 0 && (
          <Button variant="subtle" size="sm" leftSection={<Sparkles size={15} />} loading={importing} onClick={importPresets}>补全常用分类</Button>
        )}
      </Group>

      {roots.length === 0 ? (
        <Card>
          <EmptyState mood="calm" title={`还没有${CATEGORY_TYPES.find((item) => item.value === type).label}分类`}
            description="一键导入一套常用分类（都配好了 emoji），之后再按自己的习惯增删～"
            action={(
              <Group gap="sm">
                <Button leftSection={<Sparkles size={16} />} loading={importing} onClick={importPresets}>导入常用分类</Button>
                <Button variant="default" onClick={() => open(null, { categoryType: type })}>手动添加</Button>
              </Group>
            )} />
        </Card>
      ) : (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
          {roots.map((root) => (
            <RootCard key={root.id} root={root} onEdit={(category) => open(category)} onDelete={remove}
              onAddChild={(parent) => open(null, { categoryType: type, pid: parent.id })} />
          ))}
        </SimpleGrid>
      )}

      <CategoryModal opened={editor.opened} category={editor.category} defaults={editor.defaults}
        onClose={() => setEditor((current) => ({ ...current, opened: false }))} />
      <ConfirmModal state={confirmState} onClose={() => setConfirmState(null)} />
    </>
  );
}
