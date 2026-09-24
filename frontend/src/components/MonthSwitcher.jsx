import { ActionIcon, Button, Group, Text } from '@mantine/core';
import { ChevronLeft, ChevronRight } from '../lib/icons';
import { currentMonth, formatMonth, isSameMonth, shiftMonth } from '../lib/format';

export function MonthSwitcher({ value, onChange }) {
  const thisMonth = currentMonth();
  return (
    <Group gap={4} wrap="nowrap">
      <ActionIcon variant="light" size={36} onClick={() => onChange(shiftMonth(value, -1))} aria-label="上个月">
        <ChevronLeft size={16} />
      </ActionIcon>
      <Text miw={120} px={4} ta="center" className="num" style={{ whiteSpace: 'nowrap' }}>📅 {formatMonth(value)}</Text>
      <ActionIcon variant="light" size={36} onClick={() => onChange(shiftMonth(value, 1))} aria-label="下个月"
        disabled={isSameMonth(value, thisMonth)}>
        <ChevronRight size={16} />
      </ActionIcon>
      {!isSameMonth(value, thisMonth) && (
        <Button variant="subtle" size="compact-sm" onClick={() => onChange(thisMonth)}>本月</Button>
      )}
    </Group>
  );
}
