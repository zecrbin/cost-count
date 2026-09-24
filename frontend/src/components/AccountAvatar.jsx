import { useState } from 'react';

// 和后端兜底图标一样按名称哈希取色相，同一提供方颜色稳定
function hue(name) {
  let hash = 0;
  for (const ch of name) hash = (hash * 31 + ch.codePointAt(0)) | 0;
  return ((hash % 360) + 360) % 360;
}

function Fallback({ name, size }) {
  const text = (name || '账').trim();
  return (
    <div
      className="avatar avatar-fallback"
      style={{ width: size, height: size, fontSize: size * 0.42, background: `hsl(${hue(text)} 38% 42%)` }}
      aria-hidden="true"
    >
      {text.slice(0, 1)}
    </div>
  );
}

/**
 * 账户头像：优先用系统生成的账户图标（提供方图标 + 尾号），
 * 没有尾号时用提供方图标，都没有或加载失败时退回首字色块。
 */
export function AccountAvatar({ account, size = 48 }) {
  // 后端每次重绘图标都会换新文件名，路径变了浏览器自然会取新图，不需要额外绕缓存
  const sources = [];
  if (account.icon) sources.push(`/icons/${account.icon}`);
  if (account.providerIcon) sources.push(`/icons/default/${account.providerIcon}`);
  return <ImageStack sources={sources} name={account.providerName || account.accName} size={size} />;
}

export function ProviderAvatar({ provider, size = 44 }) {
  const sources = provider.icon ? [`/icons/default/${provider.icon}`] : [];
  return <ImageStack sources={sources} name={provider.providerName} size={size} />;
}

function ImageStack({ sources, name, size }) {
  const [failed, setFailed] = useState(0);
  const key = sources.join('|');
  const [lastKey, setLastKey] = useState(key);
  if (key !== lastKey) {
    setLastKey(key);
    setFailed(0);
  }
  const src = sources[failed];
  if (!src) return <Fallback name={name} size={size} />;
  return (
    <div className="avatar" style={{ width: size, height: size, borderRadius: size * 0.27 }}>
      <img src={src} alt="" loading="lazy" onError={() => setFailed((n) => n + 1)} />
    </div>
  );
}
