import { Button, FileButton, Group, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { api } from '../api';
import { notifyError } from '../lib/notify';
import { AccountAvatar } from './AccountAvatar';

const ACCEPT = 'image/png,image/jpeg,image/gif,image/webp';
const MAX_BYTES = 2 * 1024 * 1024;

/**
 * 图标上传：左侧预览，右侧上传 / 恢复默认。
 * value 为上传后的相对路径，为空表示使用默认图标；previewAccount 用于渲染默认图标预览。
 */
export function IconUpload({ label = '图标', value, onChange, previewAccount, defaultHint }) {
  const [uploading, setUploading] = useState(false);

  const upload = async (file) => {
    if (!file) return;
    if (file.size > MAX_BYTES) {
      notifyError(new Error('图片不能超过 2MB'), '上传失败');
      return;
    }
    setUploading(true);
    try {
      onChange(await api.uploadIcon(file));
    } catch (error) {
      notifyError(error, '上传失败');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <Text size="sm" fw={500} mb={6}>{label}</Text>
      <Group gap="md" wrap="nowrap" align="center" p="sm"
        style={{ background: 'var(--cc-surface-muted)', borderRadius: 18 }}>
        <AccountAvatar account={{ ...previewAccount, ...(value ? { icon: value } : {}) }} size={56} radius="lg" />
        <Stack gap={6} style={{ flex: 1, minWidth: 0 }}>
          <Group gap="xs">
            <FileButton accept={ACCEPT} onChange={upload}>
              {(props) => <Button {...props} size="xs" variant="light" loading={uploading}>{value ? '换一张' : '上传图片'}</Button>}
            </FileButton>
            {value && <Button size="xs" variant="subtle" color="gray" onClick={() => onChange(null)}>恢复默认</Button>}
          </Group>
          <Text size="xs" c="dimmed">
            {value ? '已使用上传的图片' : defaultHint}，支持 PNG / JPG / GIF / WEBP，2MB 以内
          </Text>
        </Stack>
      </Group>
    </div>
  );
}
