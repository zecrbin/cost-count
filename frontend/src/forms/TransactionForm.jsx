import { useMemo, useState } from 'react';
import { ChevronRight, Info, LoaderCircle } from 'lucide-react';
import { Field, Modal } from '../components/ui';
import { useStore } from '../lib/store';
import { api } from '../lib/api';
import {
  fromInputDateTime,
  isCredit,
  isValidAmount,
  requestId as newRequestId,
  sanitizeAmount,
  toInputDateTime,
} from '../lib/format';

const TYPES = [
  { key: 'EXPENSE', label: '支出' },
  { key: 'INCOME', label: '收入' },
  { key: 'TRANSFER', label: '转账' },
  { key: 'ADJUSTMENT', label: '调整' },
];

const AMOUNT_COLOR = { EXPENSE: 'var(--ink)', INCOME: 'var(--jade)', TRANSFER: 'var(--ink)', ADJUSTMENT: 'var(--gold)' };

function AccountOptions({ accounts, exclude }) {
  const assets = accounts.filter((a) => !isCredit(a.typeCode) && a.id !== exclude);
  const credits = accounts.filter((a) => isCredit(a.typeCode) && a.id !== exclude);
  return (
    <>
      <option value="">选择账户</option>
      {assets.length > 0 && (
        <optgroup label="资产账户">
          {assets.map((a) => (
            <option key={a.id} value={a.id}>
              {a.accName}
            </option>
          ))}
        </optgroup>
      )}
      {credits.length > 0 && (
        <optgroup label="信用账户">
          {credits.map((a) => (
            <option key={a.id} value={a.id}>
              {a.accName}
            </option>
          ))}
        </optgroup>
      )}
    </>
  );
}

function CategoryPicker({ groups, value, onChange }) {
  const selectedGroup = groups.find((g) => g.id === value || g.children.some((c) => c.id === value));
  const [openId, setOpenId] = useState(selectedGroup?.id ?? null);
  const open = groups.find((g) => g.id === openId);
  const selectedChild = selectedGroup?.children.find((c) => c.id === value);

  return (
    <div>
      <div className="chips">
        {groups.map((group) => (
          <button
            type="button"
            key={group.id}
            className={`chip group-chip${group.id === value ? ' selected' : ''}${group.id === openId ? ' open' : ''}`}
            onClick={() => {
              setOpenId(group.id);
              onChange(group.id);
            }}
          >
            {group.categoryName}
          </button>
        ))}
      </div>
      {open && open.children.length > 0 && (
        <div className="children" key={open.id}>
          {open.children.map((child) => (
            <button
              type="button"
              key={child.id}
              className={`chip${child.id === value ? ' selected' : ''}`}
              onClick={() => onChange(child.id)}
            >
              {child.categoryName}
            </button>
          ))}
        </div>
      )}
      {selectedGroup && (
        <div className="picked" style={{ marginTop: 8 }}>
          已选
          <strong>{selectedGroup.categoryName}</strong>
          {selectedChild && (
            <>
              <ChevronRight size={13} />
              <strong>{selectedChild.categoryName}</strong>
            </>
          )}
        </div>
      )}
    </div>
  );
}

export function TransactionForm({ presetAccountId, onClose }) {
  const { accounts, categoryIndex, refreshAccounts, bumpLedger, toast } = useStore();
  const usable = useMemo(() => accounts.filter((a) => a.status !== 0), [accounts]);

  const [type, setType] = useState('EXPENSE');
  const [amount, setAmount] = useState('');
  const [adjustSign, setAdjustSign] = useState(1);
  const [accountId, setAccountId] = useState(presetAccountId && usable.some((a) => a.id === presetAccountId) ? presetAccountId : usable[0]?.id ?? '');
  const [targetAccountId, setTargetAccountId] = useState('');
  const [categoryId, setCategoryId] = useState(null);
  const [time, setTime] = useState(() => toInputDateTime(new Date()));
  const [counterparty, setCounterparty] = useState('');
  const [remark, setRemark] = useState('');
  const [requestId] = useState(newRequestId);
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const groups = type === 'EXPENSE' || type === 'INCOME' ? categoryIndex.tree[type] ?? [] : [];
  const nowInput = toInputDateTime(new Date());

  const errors = {};
  if (!isValidAmount(amount)) errors.amount = type === 'ADJUSTMENT' ? '请输入调整金额' : '请输入大于 0 的金额';
  if (!accountId) errors.account = '请选择账户';
  if (type === 'TRANSFER' && !targetAccountId) errors.target = '请选择转入账户';
  if ((type === 'EXPENSE' || type === 'INCOME') && !categoryId) errors.category = '请选择分类';
  if (!time) errors.time = '请选择发生时间';
  else if (time > nowInput) errors.time = '发生时间不能晚于当前时间';
  const valid = Object.keys(errors).length === 0;

  function switchType(next) {
    setType(next);
    setCategoryId(null);
    if (next !== 'TRANSFER') setTargetAccountId('');
  }

  async function submit(event) {
    event.preventDefault();
    setSubmitted(true);
    if (!valid || busy) return;
    setBusy(true);
    try {
      const signed = type === 'ADJUSTMENT' ? (adjustSign < 0 ? `-${amount}` : amount) : null;
      await api.createTransaction({
        transactionType: type,
        accountId,
        targetAccountId: type === 'TRANSFER' ? targetAccountId : null,
        categoryId: type === 'EXPENSE' || type === 'INCOME' ? categoryId : null,
        amount: type === 'ADJUSTMENT' ? null : amount,
        balanceChange: signed,
        transactionTime: fromInputDateTime(time),
        counterparty: counterparty.trim() || null,
        remark: remark.trim() || null,
        requestId,
      });
      await refreshAccounts();
      bumpLedger();
      toast('已入账');
      onClose();
    } catch (error) {
      toast(error.message, 'error');
      setBusy(false);
    }
  }

  const show = (key) => submitted && errors[key];

  return (
    <Modal title="记一笔" onClose={onClose} wide>
      <form onSubmit={submit} style={{ display: 'grid', gap: 20 }} noValidate>
        <div className="type-tabs" role="tablist">
          {TYPES.map((t) => (
            <button
              type="button"
              key={t.key}
              role="tab"
              aria-selected={type === t.key}
              className={`t-${t.key}${type === t.key ? ' active' : ''}`}
              onClick={() => switchType(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {type === 'ADJUSTMENT' && (
          <div className="note" style={{ marginTop: -6 }}>
            <Info size={14} style={{ flex: 'none', marginTop: 2 }} />
            用于修正账面与实际余额的小额偏差，比如利率变动后按旧利率算出的利息差额。正常的利息请记为收入。
          </div>
        )}

        <div>
          <div className="amount-input">
            <span className="currency">¥</span>
            <input
              inputMode="decimal"
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(sanitizeAmount(e.target.value))}
              style={{ color: AMOUNT_COLOR[type] }}
              aria-label="金额"
            />
            {type === 'ADJUSTMENT' && (
              <div className="toggle" role="group" aria-label="调整方向">
                <button type="button" className={adjustSign > 0 ? 'active' : ''} onClick={() => setAdjustSign(1)}>
                  增加
                </button>
                <button type="button" className={adjustSign < 0 ? 'active' : ''} onClick={() => setAdjustSign(-1)}>
                  减少
                </button>
              </div>
            )}
          </div>
          {show('amount') && <span className="hint error">{errors.amount}</span>}
        </div>

        <div className="field-row">
          <Field label={type === 'TRANSFER' ? '转出账户' : '账户'} error={show('account') && errors.account}>
            <select className="control" value={accountId} onChange={(e) => setAccountId(e.target.value)}>
              <AccountOptions accounts={usable} />
            </select>
          </Field>
          {type === 'TRANSFER' ? (
            <Field label="转入账户" error={show('target') && errors.target}>
              <select className="control" value={targetAccountId} onChange={(e) => setTargetAccountId(e.target.value)}>
                <AccountOptions accounts={usable} exclude={accountId} />
              </select>
            </Field>
          ) : (
            <Field label="发生时间" error={show('time') && errors.time}>
              <input
                type="datetime-local"
                className="control"
                value={time}
                max={nowInput}
                onChange={(e) => setTime(e.target.value)}
              />
            </Field>
          )}
        </div>

        {type === 'TRANSFER' && (
          <Field label="发生时间" error={show('time') && errors.time}>
            <input
              type="datetime-local"
              className="control"
              value={time}
              max={nowInput}
              onChange={(e) => setTime(e.target.value)}
            />
          </Field>
        )}

        {groups.length > 0 && (
          <div className="field">
            <span className="field-label">
              分类 <small>一级、二级分类都可以直接选</small>
            </span>
            <CategoryPicker key={type} groups={groups} value={categoryId} onChange={setCategoryId} />
            {show('category') && <span className="hint error">{errors.category}</span>}
          </div>
        )}

        {(type === 'EXPENSE' || type === 'INCOME') && (
          <Field label="交易对象" aside="选填">
            <input
              className="control"
              placeholder="商户、公司或个人"
              maxLength={256}
              value={counterparty}
              onChange={(e) => setCounterparty(e.target.value)}
            />
          </Field>
        )}

        <Field label="备注" aside="选填">
          <textarea className="control" maxLength={256} value={remark} onChange={(e) => setRemark(e.target.value)} />
        </Field>

        <div className="note" style={{ marginTop: -4 }}>
          <Info size={14} style={{ flex: 'none', marginTop: 2 }} />
          发生时间早于账户最近一笔流水时，系统会自动把它补进历史，并重算之后每一笔的余额。
        </div>

        <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
          {busy && <LoaderCircle size={17} className="spin" />}
          入账
        </button>
      </form>
    </Modal>
  );
}
