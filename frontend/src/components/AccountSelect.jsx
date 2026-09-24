import { Group, Select, Text } from '@mantine/core';
import { useMemo } from 'react';
import { useData } from '../lib/data';
import { ACCOUNT_KINDS } from '../lib/meta';
import { AccountAvatar } from './AccountAvatar';
import { Money } from './Money';

/** 按资产/负债分组的账户选择器；默认只列出启用中的账户，includeIds 用于编辑时保留已停用的原账户。 */
export function AccountSelect({ value, onChange, includeIds = [], excludeId, ...props }) {
  const { accounts, accountMap } = useData();
  const data = useMemo(() => {
    const visible = accounts.filter((account) =>
      account.id !== excludeId && (account.status !== 0 || includeIds.includes(account.id)));
    return Object.entries(ACCOUNT_KINDS)
      .map(([code, kind]) => ({
        group: kind.label,
        items: visible.filter((account) => (account.typeCode || 'DEBIT') === code).map((account) => ({
          value: account.id,
          label: account.accTailNum ? `${account.accName} (${account.accTailNum})` : account.accName,
        })),
      }))
      .filter((group) => group.items.length);
  }, [accounts, excludeId, includeIds]);

  return (
    <Select
      data={data}
      value={value}
      onChange={onChange}
      searchable
      // 再次点击已选账户时保持选中；需要清空时使用 clearable 的清除按钮。
      allowDeselect={false}
      nothingFoundMessage="没有匹配的账户"
      leftSection={value && accountMap.get(value) ? <AccountAvatar account={accountMap.get(value)} size={20} radius="sm" /> : null}
      renderOption={({ option }) => {
        const account = accountMap.get(option.value);
        return (
          <Group gap="sm" wrap="nowrap" w="100%">
            <AccountAvatar account={account} size={26} radius="sm" />
            <Text size="sm" style={{ flex: 1 }} className="cc-truncate">{option.label}</Text>
            <Money value={account?.balance} size="xs" c="dimmed" />
          </Group>
        );
      }}
      {...props}
    />
  );
}
