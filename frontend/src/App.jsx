import {
  ActionIcon, AppShell, Burger, Button, Center, Group, Loader, Text, Tooltip, useComputedColorScheme,
  useMantineColorScheme,
} from '@mantine/core';
import { useDisclosure, useMediaQuery } from '@mantine/hooks';
import { Moon, Plus, Sun } from './lib/icons';
import { useEffect, useState } from 'react';
import { EmptyState } from './components/EmptyState';
import { Mascot } from './components/Mascot';
import { useData } from './lib/data';
import { useTransactionEditor } from './lib/editor';
import { currentPageId, navigate } from './lib/navigation';
import { AccountsPage } from './pages/AccountsPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { DashboardPage } from './pages/DashboardPage';
import { SettingsPage } from './pages/SettingsPage';
import { TransactionsPage } from './pages/TransactionsPage';

const PAGES = [
  { id: 'dashboard', label: '总览', emoji: '🏡', component: DashboardPage },
  { id: 'transactions', label: '流水', emoji: '🧾', component: TransactionsPage },
  { id: 'accounts', label: '账户', emoji: '👛', component: AccountsPage },
  { id: 'categories', label: '分类', emoji: '🏷️', component: CategoriesPage },
  { id: 'settings', label: '机构与类型', emoji: '🏦', component: SettingsPage },
];

function ColorSchemeToggle() {
  const { setColorScheme } = useMantineColorScheme();
  const scheme = useComputedColorScheme('light');
  return (
    <Tooltip label={scheme === 'dark' ? '切到白天' : '切到夜晚'}>
      <ActionIcon variant="light" size={38} aria-label="切换配色"
        onClick={() => setColorScheme(scheme === 'dark' ? 'light' : 'dark')}>
        {scheme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
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
      header={{ height: 60, collapsed: !isMobile }}
      navbar={{ width: 256, breakpoint: 'sm', collapsed: { mobile: !navOpened } }}
      padding={0}
    >
      <AppShell.Header className="cc-header">
        <Group h="100%" px="md" justify="space-between">
          <Group gap="xs">
            <Burger opened={navOpened} onClick={nav.toggle} size="sm" aria-label="打开导航" />
            <Mascot size={34} mood="happy" />
            <Text className="cc-brand-name">Cost Count</Text>
          </Group>
          <ColorSchemeToggle />
        </Group>
      </AppShell.Header>

      <AppShell.Navbar className="cc-navbar" p={isMobile ? 'sm' : 'md'} pr={isMobile ? 'sm' : 0}>
        <div className="cc-nav-panel">
          <div className="cc-brand">
            <Mascot size={46} mood="happy" />
            <div>
              <div className="cc-brand-name">Cost Count</div>
              <Text size="xs" c="dimmed" fw={600}>小猪记账本</Text>
            </div>
          </div>
          <Button size="md" leftSection={<Plus size={18} />} mb="lg" onClick={() => openTransaction()} disabled={Boolean(error)}
            variant="gradient" gradient={{ from: 'berry.6', to: '#4f8ef7', deg: 135 }}>
            记一笔
          </Button>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {PAGES.map(({ id, label, emoji }) => (
              <button key={id} type="button" className="cc-nav-link" data-active={id === pageId || undefined}
                onClick={() => navigate(id)}>
                <span className="cc-nav-emoji">{emoji}</span>
                <span>{label}</span>
              </button>
            ))}
          </div>
          <Group mt="auto" justify="space-between" pt="md" visibleFrom="sm">
            <Text size="xs" c="dimmed" fw={600}>每一分钱都有去处 🌱</Text>
            <ColorSchemeToggle />
          </Group>
        </div>
      </AppShell.Navbar>

      <AppShell.Main className="cc-main">
        <div className="cc-content">
          {loading ? (
            <Center h="60vh"><Loader type="dots" size="lg" /></Center>
          ) : error ? (
            <EmptyState mood="sad" title="连不上记账服务" description={`${error.message}。确认后端已在 8081 端口启动后再试一次吧。`}
              action={<Button onClick={loadAll}>重新连接</Button>} py={120} />
          ) : (
            <div className="cc-page" key={pageId}><PageComponent /></div>
          )}
        </div>
      </AppShell.Main>

      {isMobile && !error && (
        <ActionIcon className="cc-fab" size={60} radius="xl" onClick={() => openTransaction()} aria-label="记一笔">
          <Plus size={28} />
        </ActionIcon>
      )}
    </AppShell>
  );
}
