import { useCallback, useEffect, useMemo, useState } from 'react';
import { CircleAlert, Library, Plus, ReceiptText, RefreshCw, WalletCards } from 'lucide-react';
import { useStore } from './lib/store';
import { todayParts } from './lib/format';
import { ConfirmDialog, Loading, Toasts } from './components/ui';
import { AccountsPage } from './pages/AccountsPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { SettingsPage } from './pages/SettingsPage';
import { TransactionForm } from './forms/TransactionForm';
import { AccountForm } from './forms/AccountForm';
import { BalanceForm } from './forms/BalanceForm';
import { ProviderForm, TypeForm } from './forms/DictionaryForms';

const PAGES = [
  { key: 'accounts', label: '资金总览', icon: WalletCards },
  { key: 'ledger', label: '流水账', icon: ReceiptText },
  { key: 'settings', label: '账户字典', icon: Library },
];

function readHash() {
  const key = window.location.hash.replace(/^#\/?/, '');
  return PAGES.some((p) => p.key === key) ? key : 'accounts';
}

export function App() {
  const { accounts, ready, offline, loadAll, toast } = useStore();
  const [page, setPage] = useState(readHash);
  // 从账户卡片跳到流水时带上的账户筛选；换 key 让流水页按新的初始条件重新挂载
  const [ledgerAccount, setLedgerAccount] = useState('');
  const [ledgerKey, setLedgerKey] = useState(0);
  const [modal, setModal] = useState(null);
  const today = todayParts();

  useEffect(() => {
    const onHash = () => setPage(readHash());
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  const go = useCallback((key) => {
    window.location.hash = `/${key}`;
    setPage(key);
  }, []);

  const openLedger = useCallback(
    (accountId) => {
      setLedgerAccount(accountId);
      setLedgerKey((k) => k + 1);
      go('ledger');
    },
    [go],
  );

  const close = useCallback(() => setModal(null), []);

  const ui = useMemo(
    () => ({
      recordTransaction: (presetAccountId) => {
        if (!accounts.some((a) => a.status !== 0)) {
          toast('先新建一个账户再记账', 'error');
          setModal({ kind: 'account', account: null });
          return;
        }
        setModal({ kind: 'tx', presetAccountId });
      },
      editAccount: (account) => setModal({ kind: 'account', account }),
      fixBalance: (account) => setModal({ kind: 'balance', account }),
      editProvider: (provider) => setModal({ kind: 'provider', provider }),
      editType: (type, presetProviderId) => setModal({ kind: 'type', type, presetProviderId }),
      confirm: (options) => setModal({ kind: 'confirm', ...options }),
    }),
    [accounts, toast],
  );

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <div className="seal" aria-hidden="true">
            账
          </div>
          <div>
            <div className="brand-name">Cost Count</div>
            <div className="brand-sub">个人账簿</div>
          </div>
        </div>

        <nav className="nav" aria-label="主导航">
          <div className="nav-label">账簿</div>
          {PAGES.map(({ key, label, icon: Icon }) => (
            <button
              type="button"
              key={key}
              className={`nav-item${page === key ? ' active' : ''}`}
              aria-current={page === key ? 'page' : undefined}
              onClick={() => {
                if (key === 'ledger') {
                  setLedgerAccount('');
                  setLedgerKey((k) => k + 1);
                }
                go(key);
              }}
            >
              <Icon size={18} />
              <span className="label">{label}</span>
              {key === 'accounts' && ready && <span className="nav-hint">{accounts.length}</span>}
            </button>
          ))}
        </nav>

        <div className="sidebar-foot">
          <button type="button" className="btn btn-primary btn-record" onClick={() => ui.recordTransaction()}>
            <Plus size={18} />
            记一笔
          </button>
          <div className="today">
            <strong>{today.day}</strong>
            {today.line}
          </div>
        </div>
      </aside>

      <main className="main">
        {offline && (
          <div className="banner page">
            <CircleAlert size={18} />
            <span style={{ flex: 1 }}>{offline}</span>
            <button type="button" className="btn" onClick={loadAll}>
              <RefreshCw size={15} />
              重试
            </button>
          </div>
        )}
        {!ready ? (
          <Loading />
        ) : page === 'accounts' ? (
          <AccountsPage ui={ui} onOpenLedger={openLedger} />
        ) : page === 'ledger' ? (
          <TransactionsPage key={ledgerKey} initialAccountId={ledgerAccount} ui={ui} />
        ) : (
          <SettingsPage ui={ui} />
        )}
      </main>

      <button type="button" className="btn btn-primary fab" onClick={() => ui.recordTransaction()}>
        <Plus size={18} />
        记一笔
      </button>

      {modal?.kind === 'tx' && <TransactionForm presetAccountId={modal.presetAccountId} onClose={close} />}
      {modal?.kind === 'account' && <AccountForm account={modal.account} onClose={close} />}
      {modal?.kind === 'balance' && <BalanceForm account={modal.account} onClose={close} />}
      {modal?.kind === 'provider' && <ProviderForm provider={modal.provider} onClose={close} />}
      {modal?.kind === 'type' && <TypeForm type={modal.type} presetProviderId={modal.presetProviderId} onClose={close} />}
      {modal?.kind === 'confirm' && (
        <ConfirmDialog title={modal.title} message={modal.message} onConfirm={modal.onConfirm} onClose={close} />
      )}

      <Toasts />
    </div>
  );
}
