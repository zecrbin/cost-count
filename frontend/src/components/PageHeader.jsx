import { Group, Text, Title } from '@mantine/core';

export function PageHeader({ title, description, actions }) {
  return (
    <Group className="cc-page-header" justify="space-between" align="flex-end" gap="md">
      <div>
        <Title order={1} className="cc-page-title">{title}</Title>
        {description && <Text size="sm" c="dimmed" mt={6}>{description}</Text>}
      </div>
      {actions && <Group gap="sm">{actions}</Group>}
    </Group>
  );
}
