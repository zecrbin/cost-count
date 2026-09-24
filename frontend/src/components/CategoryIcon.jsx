import { ThemeIcon } from '@mantine/core';
import { CATEGORY_ICONS, TYPE_FALLBACK_ICONS, colorFromText } from '../lib/meta';

/** 分类图标：颜色取一级分类名称，保证同一大类下的明细颜色一致。 */
export function CategoryIcon({ category, type, size = 36, radius = 'xl' }) {
  const Icon = CATEGORY_ICONS[category?.icon] || CATEGORY_ICONS[category?.root?.icon] || TYPE_FALLBACK_ICONS[type] || TYPE_FALLBACK_ICONS.EXPENSE;
  const color = category ? colorFromText(category.root?.categoryName || category.categoryName) : '#868e96';
  return (
    <ThemeIcon size={size} radius={radius} variant="light" style={{ color, background: `color-mix(in srgb, ${color} 13%, transparent)` }}>
      <Icon size={Math.round(size * 0.5)} strokeWidth={2} />
    </ThemeIcon>
  );
}
