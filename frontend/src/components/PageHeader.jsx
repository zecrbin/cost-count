import { Group, Text, Title } from '@mantine/core';

export function PageHeader({ title, emoji, description, actions }) {
  return (
    <Group className="cc-page-header" justify="space-between" align="flex-end" gap="md">
      <div>
        <Title order={1} className="cc-page-title">{emoji && <span style={{ marginRight: 10 }}>{emoji}</span>}{title}</Title>
        {description && <Text size="sm" c="dimmed" fw={600} mt={6}>{description}</Text>}
      </div>
      {actions && <Group gap="sm">{actions}</Group>}
    </Group>
  );
}
