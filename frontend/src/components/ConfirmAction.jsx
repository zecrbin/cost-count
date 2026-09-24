import { Button, Group, Modal, Popover, Stack, Text } from '@mantine/core';
import { useState } from 'react';

/** 行内二次确认，用于删除等不可恢复操作。children 为触发按钮，需接收 onClick。 */
export function ConfirmAction({ message, confirmLabel = '删除', onConfirm, children, position = 'bottom-end' }) {
  const [opened, setOpened] = useState(false);
  const [busy, setBusy] = useState(false);
  const confirm = async () => {
    setBusy(true);
    try {
      await onConfirm();
      setOpened(false);
    } finally {
      setBusy(false);
    }
  };
  return (
    <Popover opened={opened} onChange={setOpened} position={position} withArrow shadow="md" radius="md" trapFocus>
      <Popover.Target>
        <span onClick={(event) => { event.stopPropagation(); setOpened((value) => !value); }}>{children}</span>
      </Popover.Target>
      <Popover.Dropdown onClick={(event) => event.stopPropagation()}>
        <Stack gap="sm" maw={240}>
          <Text size="sm">{message}</Text>
          <Group gap="xs" justify="flex-end">
            <Button size="compact-sm" variant="default" onClick={() => setOpened(false)}>取消</Button>
            <Button size="compact-sm" color="red" loading={busy} onClick={confirm}>{confirmLabel}</Button>
          </Group>
        </Stack>
      </Popover.Dropdown>
    </Popover>
  );
}

/** 确认弹窗，用于菜单项等无法挂载气泡的位置。state 为 null 时关闭。 */
export function ConfirmModal({ state, onClose }) {
  const [busy, setBusy] = useState(false);
  const confirm = async () => {
    setBusy(true);
    try {
      await state.onConfirm();
      onClose();
    } finally {
      setBusy(false);
    }
  };
  return (
    <Modal opened={Boolean(state)} onClose={onClose} size="sm" title={<Text fw={650}>{state?.title || '确认删除'}</Text>}>
      <Text size="sm">{state?.message}</Text>
      <Group justify="flex-end" gap="sm" mt="lg">
        <Button variant="default" onClick={onClose}>取消</Button>
        <Button color="red" loading={busy} onClick={confirm}>{state?.confirmLabel || '删除'}</Button>
      </Group>
    </Modal>
  );
}
