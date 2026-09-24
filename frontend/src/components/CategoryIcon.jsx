import { TYPE_EMOJI, categoryEmoji, colorFromText } from '../lib/meta';

/** 圆角 emoji 气泡；底色取一级分类名称对应的糖果色，同一大类下的明细颜色一致。 */
export function EmojiBubble({ emoji, color = '#8b5cf6', size = 40, radius }) {
  return (
    <span className="cc-emoji-bubble" style={{
      width: size, height: size, borderRadius: radius ?? Math.round(size * 0.38), fontSize: Math.round(size * 0.52),
      background: `color-mix(in srgb, ${color} 16%, transparent)`,
    }}>
      {emoji}
    </span>
  );
}

export function CategoryIcon({ category, type, size = 40 }) {
  const color = category ? colorFromText(category.root?.categoryName || category.categoryName) : '#8b84a3';
  const emoji = categoryEmoji(category) || (category ? null : TYPE_EMOJI[type] || TYPE_EMOJI.EXPENSE);
  if (emoji) return <EmojiBubble emoji={emoji} color={color} size={size} />;
  return (
    <span className="cc-emoji-bubble" style={{
      width: size, height: size, borderRadius: Math.round(size * 0.38), fontSize: Math.round(size * 0.42), fontWeight: 800,
      color, background: `color-mix(in srgb, ${color} 16%, transparent)`,
    }}>
      {category.categoryName.slice(0, 1)}
    </span>
  );
}
