import { Group, ScrollArea, Text } from '@mantine/core';
import { useEffect, useMemo, useState } from 'react';
import { useData } from '../lib/data';
import { CategoryIcon } from './CategoryIcon';

/** 两级分类选择：上方选一级分类，下方选明细；没有明细的一级分类可直接选中。 */
export function CategoryPicker({ type, value, onChange, error }) {
  const { categories, categoryMap } = useData();
  const roots = useMemo(() => categories.filter((category) => category.categoryType === type), [categories, type]);
  const selected = value ? categoryMap.get(value) : null;
  const [rootId, setRootId] = useState(selected?.root?.id || roots[0]?.id || null);

  useEffect(() => {
    const current = value ? categoryMap.get(value) : null;
    if (current?.root && current.categoryType === type) setRootId(current.root.id);
    else if (!roots.some((root) => root.id === rootId)) setRootId(roots[0]?.id || null);
  }, [value, type, roots, categoryMap]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!roots.length) {
    return <Text size="sm" c="dimmed" py="sm">还没有{type === 'INCOME' ? '收入' : '支出'}分类，请先到「分类」页添加。</Text>;
  }

  const root = roots.find((item) => item.id === rootId) || roots[0];
  const children = root.children || [];

  const selectRoot = (item) => {
    setRootId(item.id);
    if (!item.children?.length) onChange(item.id);
  };

  return (
    <div>
      <Text size="sm" fw={500} mb={6}>分类{error && <Text span c="red" size="xs" ml={8}>{error}</Text>}</Text>
      <ScrollArea type="never" offsetScrollbars={false}>
        <Group gap={6} wrap="nowrap" pb={4}>
          {roots.map((item) => (
            <button key={item.id} type="button" className="cc-chip" onClick={() => selectRoot(item)}
              data-selected={item.id === root.id || undefined} style={{ flexShrink: 0 }}>
              {item.categoryName}
            </button>
          ))}
        </Group>
      </ScrollArea>
      {children.length > 0 && (
        <Group gap={8} mt={10} p="sm" style={{ background: 'var(--cc-surface-muted)', borderRadius: 12 }}>
          <CategoryIcon category={categoryMap.get(root.id)} size={30} />
          {children.map((child) => (
            <button key={child.id} type="button" className="cc-option" data-selected={child.id === value || undefined}
              onClick={() => onChange(child.id)}>
              {categoryMap.get(child.id)?.icon && <CategoryIcon category={categoryMap.get(child.id)} size={20} />}
              {child.categoryName}
            </button>
          ))}
        </Group>
      )}
    </div>
  );
}
