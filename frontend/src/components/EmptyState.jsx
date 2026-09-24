import { Stack, Text, ThemeIcon } from '@mantine/core';
import { Inbox } from 'lucide-react';

export function EmptyState({ icon: Icon = Inbox, title, description, action, py = 48 }) {
  return (
    <Stack align="center" gap={6} py={py} px="md" ta="center">
      <ThemeIcon size={48} radius="xl" variant="light" color="gray"><Icon size={22} /></ThemeIcon>
      <Text fw={600} mt={6}>{title}</Text>
      {description && <Text size="sm" c="dimmed" maw={360}>{description}</Text>}
      {action && <div style={{ marginTop: 10 }}>{action}</div>}
    </Stack>
  );
}
