import { Stack, Text, ThemeIcon } from '@mantine/core';
import { Mascot } from './Mascot';

/** 空状态：默认由小猪吉祥物出场；传入 icon 时显示图标。 */
export function EmptyState({ icon: Icon, mood = 'sleepy', title, description, action, py = 48 }) {
  return (
    <Stack align="center" gap={6} py={py} px="md" ta="center">
      {Icon ? (
        <ThemeIcon size={52} radius="xl" variant="light"><Icon size={24} /></ThemeIcon>
      ) : (
        <Mascot size={92} mood={mood} />
      )}
      <Text fw={800} size="lg" mt={6}>{title}</Text>
      {description && <Text size="sm" c="dimmed" maw={380}>{description}</Text>}
      {action && <div style={{ marginTop: 12 }}>{action}</div>}
    </Stack>
  );
}
