import { ActionIcon, Button, Group, Text } from '@mantine/core';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { currentMonth, formatMonth, isSameMonth, shiftMonth } from '../lib/format';

export function MonthSwitcher({ value, onChange }) {
  const thisMonth = currentMonth();
  return (
    <Group gap={4} wrap="nowrap">
      <ActionIcon variant="default" size={34} radius="md" onClick={() => onChange(shiftMonth(value, -1))} aria-label="上个月">
        <ChevronLeft size={16} />
      </ActionIcon>
      <Text fw={600} w={96} ta="center" className="num">{formatMonth(value)}</Text>
      <ActionIcon variant="default" size={34} radius="md" onClick={() => onChange(shiftMonth(value, 1))} aria-label="下个月"
        disabled={isSameMonth(value, thisMonth)}>
        <ChevronRight size={16} />
      </ActionIcon>
      {!isSameMonth(value, thisMonth) && (
        <Button variant="subtle" size="compact-sm" onClick={() => onChange(thisMonth)}>本月</Button>
      )}
    </Group>
  );
}
