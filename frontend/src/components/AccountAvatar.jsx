import { Avatar } from '@mantine/core';
import { useState } from 'react';
import { accountIconUrl, colorFromText } from '../lib/meta';

/** 账户头像：优先使用账户/内置/机构图标，加载失败或没有图标时显示名称首字。 */
export function AccountAvatar({ account, size = 40, radius = 'lg' }) {
  const url = accountIconUrl(account);
  const [failedUrl, setFailedUrl] = useState(null);
  const name = account?.providerName || account?.accName || '?';
  if (url && failedUrl !== url) {
    // 系统生成的银行卡图标是 8:5 的卡面，按卡片比例显示，避免被压成方形。
    if (account?.icon?.startsWith('accounts/')) {
      return (
        <img src={url} alt={account.accName} onError={() => setFailedUrl(url)}
          style={{ width: size, height: Math.round(size * 0.625), borderRadius: 4, objectFit: 'cover', flexShrink: 0 }} />
      );
    }
    return (
      <Avatar src={url} size={size} radius={radius} alt={account?.accName} imageProps={{ onError: () => setFailedUrl(url) }}
        styles={{ image: { objectFit: 'contain' } }} />
    );
  }
  const color = colorFromText(name);
  return (
    <Avatar size={size} radius={radius} variant="filled" styles={{
      placeholder: {
        background: `linear-gradient(135deg, ${color}, color-mix(in srgb, ${color} 55%, #ffffff))`,
        color: '#fff', fontWeight: 900, fontSize: Math.round(size * 0.42),
      },
    }}>
      {name.slice(0, 1)}
    </Avatar>
  );
}
