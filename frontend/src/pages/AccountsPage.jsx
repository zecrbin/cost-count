import { ArrowRight, Pencil, Plus, Scale, Trash2 } from 'lucide-react';
import { AccountAvatar } from '../components/AccountAvatar';
import { Empty, Money, useCountUp } from '../components/ui';
import { useStore } from '../lib/store';
import { api } from '../lib/api';
import { isCredit, money, splitMoney } from '../lib/format';

function Hero({ assets, liabilities }) {
  const net = assets - liabilities;
  const animated = useCountUp(net);
  const { integer, fraction } = splitMoney(animated);
  const total = assets + Math.max(liabilities, 0);
  const assetShare = total > 0 ? (assets / total) * 100 : 100;

  return (
    <section className="sheet hero reveal">
      <div className="hero-main ledger-paper">
        <div className="hero-label">净资产</div>
        <div className="hero-value num">
          <span className="currency">¥</span>
          {net < 0 && '−'}
          {integer}
          <span className="fraction">.{fraction}</span>
        </div>
        <div className="hero-note">资产合计减去信用账户待还，按各账户当前余额计算</div>
      </div>
      <div className="hero-side">
        <div>
          <div className="stat-label">
            <span className="stat-dot" style={{ background: 'var(--jade)' }} />
            资产
          </div>
          <div className="stat-value num">¥ {money(assets)}</div>
        </div>
        <div>
          <div className="stat-label">
            <span className="stat-dot" style={{ background: 'var(--seal)' }} />
            负债 · 待还
          </div>
          <div className="stat-value num">¥ {money(liabilities)}</div>
        </div>
        <div className="ratio" aria-hidden="true">
          <span style={{ width: `${assetShare}%`, background: 'var(--jade)' }} />
          <span style={{ width: `${100 - assetShare}%`, background: 'var(--seal)' }} />
        </div>
      </div>
    </section>
  );
}

function UsageBar({ account }) {
  const limit = Number(account.creditLimit || 0);
  if (limit <= 0) return null;
  const used = Math.max(Number(account.balance || 0), 0);
  const pct = Math.min(used / limit, 1);
  const ideal = Number(account.idealCreditLimit || 0);
  const idealPct = ideal > 0 ? Math.min(ideal / limit, 1) : null;
  return (
    <div className="usage">
      <div className="usage-bar" title={idealPct != null ? `理想额度 ${money(ideal)}` : undefined}>
        <span style={{ width: `${pct * 100}%` }} />
        {idealPct != null && <i style={{ left: `calc(${idealPct * 100}% - 1px)` }} />}
      </div>
      <div className="usage-text">
        <span>已用 {(pct * 100).toFixed(1)}%</span>
        <span className="num">额度 {money(limit)}</span>
      </div>
    </div>
  );
}

function AccountCard({ account, index, ui, onOpenLedger }) {
  const { refreshAccounts, bumpLedger, toast } = useStore();
  const credit = isCredit(account.typeCode);
  const balance = Number(account.balance || 0);
  const label = credit ? (balance < 0 ? '溢缴款' : '待还') : balance < 0 ? '透支' : '余额';
  // 账户名已经包含提供方、类型和尾号，副标题只放名称里没有的信息
  const meta = [account.status === 0 && '已停用', account.remark].filter(Boolean).join(' · ');

  const stop = (fn) => (event) => {
    event.stopPropagation();
    fn();
  };

  function remove() {
    ui.confirm({
      title: '删除账户',
      message: `删除「${account.accName}」？只剩初始资金流水的账户才能删除；已经记过账的账户请改为停用。`,
      onConfirm: async () => {
        await api.deleteAccounts([account.id]);
        await refreshAccounts();
        bumpLedger();
        toast('账户已删除');
      },
    });
  }

  return (
    <article
      className={`sheet account-card reveal${account.status === 0 ? ' disabled' : ''}`}
      style={{ '--i': index + 2 }}
      onClick={() => onOpenLedger(account.id)}
      onKeyDown={(e) => e.key === 'Enter' && e.target === e.currentTarget && onOpenLedger(account.id)}
      tabIndex={0}
      aria-label={`${account.accName}，查看流水`}
    >
      <div className="account-top">
        <AccountAvatar account={account} />
        <div style={{ minWidth: 0 }}>
          <div className="account-name">{account.accName}</div>
          {meta && <div className="account-meta">{meta}</div>}
        </div>
        <div className="account-actions">
          <button type="button" className="icon-btn" onClick={stop(() => ui.fixBalance(account))} aria-label="修正余额" title="修正余额">
            <Scale size={16} />
          </button>
          <button type="button" className="icon-btn" onClick={stop(() => ui.editAccount(account))} aria-label="编辑账户" title="编辑">
            <Pencil size={16} />
          </button>
          <button type="button" className="icon-btn danger" onClick={stop(remove)} aria-label="删除账户" title="删除">
            <Trash2 size={16} />
          </button>
        </div>
      </div>

      <div>
        <div className="account-balance-label">{label}</div>
        <div className={`account-balance${credit && balance < 0 ? ' negative' : ''}`}>
          <Money value={balance} sign={!credit && balance < 0 ? '−' : ''} />
        </div>
      </div>

      {credit && <UsageBar account={account} />}

      <div className="account-foot">
        <span>{credit ? '信用账户' : '资产账户'}</span>
        <span className="go-ledger">
          查看流水 <ArrowRight size={13} />
        </span>
      </div>
    </article>
  );
}

function Group({ title, caption, accounts, total, totalLabel, startIndex, ui, onOpenLedger, addLabel, onAdd }) {
  return (
    <section className="section">
      <div className="section-head">
        <h2 className="section-title">
          {title}
          <small>
            {accounts.length} 个 · {caption}
          </small>
        </h2>
        <div className="section-total num">
          {totalLabel} ¥ {money(total)}
        </div>
      </div>
      <div className="account-grid">
        {accounts.map((account, i) => (
          <AccountCard key={account.id} account={account} index={startIndex + i} ui={ui} onOpenLedger={onOpenLedger} />
        ))}
        {onAdd && (
          <button type="button" className="add-tile reveal" style={{ '--i': startIndex + accounts.length + 2 }} onClick={onAdd}>
            <span className="add-ring">
              <Plus size={20} />
            </span>
            {addLabel}
          </button>
        )}
      </div>
    </section>
  );
}

export function AccountsPage({ ui, onOpenLedger }) {
  const { accounts } = useStore();
  const assetsList = accounts.filter((a) => !isCredit(a.typeCode));
  const creditList = accounts.filter((a) => isCredit(a.typeCode));
  const assets = assetsList.reduce((sum, a) => sum + Number(a.balance || 0), 0);
  const liabilities = creditList.reduce((sum, a) => sum + Number(a.balance || 0), 0);

  return (
    <div className="page">
      <header className="page-head reveal">
        <div>
          <div className="kicker">BALANCE SHEET</div>
          <h1 className="page-title">资金总览</h1>
        </div>
        <button type="button" className="btn" onClick={() => ui.editAccount(null)}>
          <Plus size={16} />
          新建账户
        </button>
      </header>

      {accounts.length === 0 ? (
        <div className="sheet">
          <Empty
            title="账簿还是空的"
            action={
              <button type="button" className="btn btn-primary" onClick={() => ui.editAccount(null)}>
                <Plus size={16} />
                新建第一个账户
              </button>
            }
          >
            先添加银行卡、支付宝这些账户，再开始记账
          </Empty>
        </div>
      ) : (
        <>
          <Hero assets={assets} liabilities={liabilities} />
          <Group
            title="资产账户"
            caption="余额是可用资金"
            accounts={assetsList}
            total={assets}
            totalLabel="合计"
            startIndex={0}
            ui={ui}
            onOpenLedger={onOpenLedger}
            addLabel="添加账户"
            onAdd={() => ui.editAccount(null)}
          />
          {creditList.length > 0 && (
            <Group
              title="信用账户"
              caption="余额是待还金额"
              accounts={creditList}
              total={liabilities}
              totalLabel="待还"
              startIndex={assetsList.length + 1}
              ui={ui}
              onOpenLedger={onOpenLedger}
            />
          )}
        </>
      )}
    </div>
  );
}
