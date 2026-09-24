import {
  ActionIcon, AppShell, Burger, Button, Center, Group, Loader, Stack, Text, Tooltip, useComputedColorScheme,
  useMantineColorScheme,
} from '@mantine/core';
import { useDisclosure, useMediaQuery } from '@mantine/hooks';
import { LayoutDashboard, Moon, Plus, ReceiptText, Settings2, Shapes, Sun, WalletCards, WifiOff } from 'lucide-react';
import { useEffect, useState } from 'react';
import { EmptyState } from './components/EmptyState';
import { useData } from './lib/data';
import { useTransactionEditor } from './lib/editor';
import { currentPageId, navigate } from './lib/navigation';
import { AccountsPage } from './pages/AccountsPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { DashboardPage } from './pages/DashboardPage';
import { SettingsPage } from './pages/SettingsPage';
import { TransactionsPage } from './pages/TransactionsPage';

const PAGES = [
  { id: 'dashboard', label: '总览', icon: LayoutDashboard, component: DashboardPage },
  { id: 'transactions', label: '流水', icon: ReceiptText, component: TransactionsPage },
  { id: 'accounts', label: '账户', icon: WalletCards, component: AccountsPage },
  { id: 'categories', label: '分类', icon: Shapes, component: CategoriesPage },
  { id: 'settings', label: '机构与类型', icon: Settings2, component: SettingsPage },
];

function ColorSchemeToggle() {
  const { setColorScheme } = useMantineColorScheme();
  const scheme = useComputedColorScheme('light');
  return (
    <Tooltip label={scheme === 'dark' ? '浅色模式' : '深色模式'}>
      <ActionIcon variant="default" size={36} radius="md" aria-label="切换配色"
        onClick={() => setColorScheme(scheme === 'dark' ? 'light' : 'dark')}>
        {scheme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
      </ActionIcon>
    </Tooltip>
  );
}

export function App() {
  const [pageId, setPageId] = useState(currentPageId);
  const [navOpened, nav] = useDisclosure(false);
  const isMobile = useMediaQuery('(max-width: 48em)');
  const { loading, error, loadAll } = useData();
  const { openTransaction } = useTransactionEditor();

  const closeNav = nav.close;
  useEffect(() => {
    const onHashChange = () => { setPageId(currentPageId()); closeNav(); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, [closeNav]);

  const page = PAGES.find((item) => item.id === pageId);
  const PageComponent = page.component;

  return (
    <AppShell
      header={{ height: 56, collapsed: !isMobile }}
      navbar={{ width: 236, breakpoint: 'sm', collapsed: { mobile: !navOpened } }}
      padding={0}
    >
      <AppShell.Header className="cc-header">
        <Group h="100%" px="md" justify="space-between">
          <Group gap="sm">
            <Burger opened={navOpened} onClick={nav.toggle} size="sm" aria-label="打开导航" />
            <div className="cc-brand-mark" style={{ width: 26, height: 26, fontSize: 14, borderRadius: 8 }}>¥</div>
            <Text fw={650}>Cost Count</Text>
          </Group>
          <ColorSchemeToggle />
        </Group>
      </AppShell.Header>

      <AppShell.Navbar className="cc-navbar" p="md">
        <Stack gap={0} h="100%">
          <div className="cc-brand">
            <div className="cc-brand-mark">¥</div>
            <div>
              <Text fw={700} lh={1.2}>Cost Count</Text>
              <Text size="xs" c="dimmed">个人账本</Text>
            </div>
          </div>
          <Button leftSection={<Plus size={17} />} mb="lg" onClick={() => openTransaction()} disabled={Boolean(error)}>
            记一笔
          </Button>
          <Stack gap={2}>
            {PAGES.map(({ id, label, icon: Icon }) => (
              <button key={id} type="button" className="cc-nav-link" data-active={id === pageId || undefined}
                onClick={() => navigate(id)}>
                <Icon size={18} strokeWidth={id === pageId ? 2.2 : 1.8} />
                <span>{label}</span>
              </button>
            ))}
          </Stack>
          <Group mt="auto" justify="space-between" pt="md" visibleFrom="sm">
            <Text size="xs" c="dimmed">本地账本</Text>
            <ColorSchemeToggle />
          </Group>
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main className="cc-main">
        <div className="cc-content">
          {loading ? (
            <Center h="60vh"><Loader /></Center>
          ) : error ? (
            <EmptyState icon={WifiOff} title="无法连接记账服务" description={`${error.message}。确认后端已在 8081 端口启动后重试。`}
              action={<Button onClick={loadAll}>重新连接</Button>} py={120} />
          ) : (
            <PageComponent />
          )}
        </div>
      </AppShell.Main>

      {isMobile && !error && (
        <ActionIcon className="cc-fab" size={56} radius="xl" onClick={() => openTransaction()} aria-label="记一笔">
          <Plus size={26} />
        </ActionIcon>
      )}
    </AppShell>
  );
}
