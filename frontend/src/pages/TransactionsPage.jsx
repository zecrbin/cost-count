import { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowDownLeft, ArrowLeftRight, ArrowRight, Flag, LoaderCircle, Plus, RotateCcw, Scale, ArrowUpRight } from 'lucide-react';
import { Empty, Loading, Money } from '../components/ui';
import { useStore } from '../lib/store';
import { api } from '../lib/api';
import { dayKey, describeDay, isCredit, money, timeOf } from '../lib/format';

const PAGE_SIZE = 30;

const TYPE_FILTERS = [
  ['', '全部'],
  ['EXPENSE', '支出'],
  ['INCOME', '收入'],
  ['TRANSFER', '转账'],
  ['ADJUSTMENT', '调整'],
];

const TYPE_META = {
  EXPENSE: { icon: ArrowUpRight, tone: 'tone-ink', title: '支出' },
  INCOME: { icon: ArrowDownLeft, tone: 'tone-jade', title: '收入' },
  TRANSFER: { icon: ArrowLeftRight, tone: 'tone-muted', title: '转账' },
  ADJUSTMENT: { icon: Scale, tone: 'tone-gold', title: '余额调整' },
  INITIAL: { icon: Flag, tone: 'tone-seal', title: '初始资金' },
};

/** 一笔流水在当前视角下的金额展示：筛选了账户时，转账按该账户是转出还是转入决定方向。 */
function amountView(tx, viewAccount) {
  const amount = Number(tx.amount || 0);
  switch (tx.transactionType) {
    case 'INCOME':
      return { sign: '+', className: 'amount-in', value: amount };
    case 'EXPENSE':
      return { sign: '−', className: 'amount-out', value: amount };
    case 'ADJUSTMENT': {
      const change = Number(tx.balanceChange || 0);
      return { sign: change < 0 ? '−' : '+', className: 'amount-adjust', value: change };
    }
    case 'TRANSFER':
      if (viewAccount && viewAccount === tx.targetAccountId) return { sign: '+', className: 'amount-in', value: amount };
      if (viewAccount && viewAccount === tx.accountId) return { sign: '−', className: 'amount-out', value: amount };
      return { sign: '', className: 'amount-neutral', value: amount };
    default:
      return { sign: '', className: 'amount-neutral', value: amount };
  }
}

function TxRow({ tx, viewAccount, categoryIndex }) {
  const meta = TYPE_META[tx.transactionType] ?? TYPE_META.EXPENSE;
  const Icon = meta.icon;
  const category = tx.categoryId ? categoryIndex.byId.get(tx.categoryId) : null;
  const view = amountView(tx, viewAccount);
  const sub = [tx.counterparty, tx.remark].filter(Boolean).join(' · ');
  const showAfter = viewAccount && tx.accountId === viewAccount && tx.balanceAfter != null;

  return (
    <div className="tx">
      <div className="tx-time num">{timeOf(tx.transactionTime)}</div>
      <div className={`tx-badge ${meta.tone}`}>
        <Icon size={17} />
      </div>
      <div style={{ minWidth: 0 }}>
        <div className="tx-title">
          {category ? category.name : meta.title}
          {category?.parentName && <span className="parent">{category.parentName}</span>}
        </div>
        <div className="tx-sub">{sub || (category ? meta.title : '—')}</div>
      </div>
      <div className="tx-account">
        <span>{tx.accountName}</span>
        {tx.transactionType === 'TRANSFER' && (
          <>
            <ArrowRight size={13} style={{ flex: 'none' }} />
            <span>{tx.targetAccountName}</span>
          </>
        )}
      </div>
      <div className="tx-amount">
        <div className={`value ${view.className}`}>
          <Money value={view.value} sign={view.sign} />
        </div>
        {showAfter && <div className="after num">余额 {money(tx.balanceAfter)}</div>}
      </div>
    </div>
  );
}

function DayBlock({ dayKeyValue, rows, index, viewAccount, categoryIndex }) {
  const { day, month, relative } = describeDay(dayKeyValue);
  let income = 0;
  let expense = 0;
  for (const tx of rows) {
    if (tx.transactionType === 'INCOME') income += Number(tx.amount || 0);
    if (tx.transactionType === 'EXPENSE') expense += Number(tx.amount || 0);
  }
  return (
    <section className="sheet day reveal" style={{ '--i': Math.min(index, 8) }}>
      <div className="day-head">
        <div className="day-num num">{day}</div>
        <div className="day-meta">
          <strong>{relative}</strong>
          {month}
        </div>
        <div className="day-sum">
          {income > 0 && (
            <span>
              收 <b className="num amount-in">+{money(income)}</b>
            </span>
          )}
          {expense > 0 && (
            <span>
              支 <b className="num">−{money(expense)}</b>
            </span>
          )}
        </div>
      </div>
      {rows.map((tx) => (
        <TxRow key={tx.id} tx={tx} viewAccount={viewAccount} categoryIndex={categoryIndex} />
      ))}
    </section>
  );
}

export function TransactionsPage({ initialAccountId, ui }) {
  const { accounts, categoryIndex, ledgerVersion } = useStore();
  const [accountId, setAccountId] = useState(initialAccountId ?? '');
  const [type, setType] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [records, setRecords] = useState([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  const requestVersion = useRef(0);

  const params = useMemo(
    () => ({
      accountId: accountId || null,
      transactionType: type || null,
      startTime: start ? `${start}T00:00:00` : null,
      endTime: end ? `${end}T23:59:59` : null,
    }),
    [accountId, type, start, end],
  );

  useEffect(() => {
    let cancelled = false;
    requestVersion.current += 1;
    setLoading(true);
    setLoadingMore(false);
    setError(null);
    api
      .pageTransactions(1, PAGE_SIZE, params)
      .then((page) => {
        if (cancelled) return;
        setRecords(page.records);
        setTotal(page.total);
        setPageNum(1);
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [params, ledgerVersion]);

  async function loadMore() {
    const version = requestVersion.current;
    setLoadingMore(true);
    try {
      const page = await api.pageTransactions(pageNum + 1, PAGE_SIZE, params);
      if (version !== requestVersion.current) return;
      setRecords((list) => [...list, ...page.records]);
      setTotal(page.total);
      setPageNum(pageNum + 1);
    } catch (err) {
      if (version === requestVersion.current) setError(err.message);
    } finally {
      if (version === requestVersion.current) setLoadingMore(false);
    }
  }

  const days = useMemo(() => {
    const map = new Map();
    for (const tx of records) {
      const key = dayKey(tx.transactionTime);
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(tx);
    }
    return [...map.entries()];
  }, [records]);

  const filtered = accountId || type || start || end;
  const selected = accounts.find((a) => a.id === accountId);

  return (
    <div className="page">
      <header className="page-head reveal">
        <div>
          <div className="kicker">LEDGER</div>
          <h1 className="page-title">流水账</h1>
          {selected && (
            <p className="page-desc">
              {selected.accName} · 当前{isCredit(selected.typeCode) ? '待还' : '余额'} ¥ {money(selected.balance)}
            </p>
          )}
        </div>
        <button type="button" className="btn btn-primary" onClick={() => ui.recordTransaction(accountId || undefined)}>
          <Plus size={16} />
          记一笔
        </button>
      </header>

      <div className="sheet toolbar reveal" style={{ '--i': 1 }}>
        <select className="control" value={accountId} onChange={(e) => setAccountId(e.target.value)} aria-label="按账户筛选">
          <option value="">全部账户</option>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.accName}
            </option>
          ))}
        </select>
        <div className="segmented" role="group" aria-label="按类型筛选">
          {TYPE_FILTERS.map(([key, label]) => (
            <button type="button" key={key || 'all'} className={type === key ? 'active' : ''} onClick={() => setType(key)}>
              {label}
            </button>
          ))}
        </div>
        <div className="grow" />
        <div className="date-range">
          <input type="date" className="control" value={start} max={end || undefined} onChange={(e) => setStart(e.target.value)} aria-label="开始日期" />
          至
          <input type="date" className="control" value={end} min={start || undefined} onChange={(e) => setEnd(e.target.value)} aria-label="结束日期" />
        </div>
        {filtered && (
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => {
              setAccountId('');
              setType('');
              setStart('');
              setEnd('');
            }}
          >
            <RotateCcw size={15} />
            重置
          </button>
        )}
      </div>

      {error && <div className="banner">{error}</div>}

      {loading ? (
        <Loading />
      ) : records.length === 0 ? (
        <div className="sheet">
          <Empty
            title={filtered ? '没有符合条件的流水' : '还没有流水'}
            action={
              !filtered && (
                <button type="button" className="btn btn-primary" onClick={() => ui.recordTransaction()}>
                  <Plus size={16} />
                  记第一笔
                </button>
              )
            }
          >
            {filtered ? '换个筛选条件试试' : '收入、支出、转账都从「记一笔」开始'}
          </Empty>
        </div>
      ) : (
        <>
          {days.map(([key, rows], i) => (
            <DayBlock key={key} dayKeyValue={key} rows={rows} index={i + 2} viewAccount={accountId} categoryIndex={categoryIndex} />
          ))}
          <div className="list-foot">
            <span>
              已显示 {records.length} / {total} 笔
            </span>
            {records.length < total && (
              <button type="button" className="btn" onClick={loadMore} disabled={loadingMore}>
                {loadingMore && <LoaderCircle size={15} className="spin" />}
                加载更多
              </button>
            )}
          </div>
        </>
      )}
    </div>
  );
}
