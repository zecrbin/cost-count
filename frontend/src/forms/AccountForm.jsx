import { useMemo, useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { Field, Modal } from '../components/ui';
import { useStore } from '../lib/store';
import { api } from '../lib/api';
import { fromInputDateTime, isCredit, sanitizeAmount, toInputDateTime } from '../lib/format';

/** 与后端 buildAccName 同一规则：类型名已带提供方名时不重复拼接。 */
function previewName(type, tail) {
  if (!type) return '';
  const provider = (type.providerName || '').trim();
  const typeName = (type.typeName || '').trim();
  return (typeName.startsWith(provider) ? '' : provider) + typeName + (tail || '');
}

export function AccountForm({ account, onClose }) {
  const { types, refreshAccounts, bumpLedger, toast } = useStore();
  const editing = Boolean(account);

  const [typeId, setTypeId] = useState(account?.typeId ?? '');
  const [tail, setTail] = useState(account?.accTailNum ?? '');
  const [initialBalance, setInitialBalance] = useState('');
  const [initialTime, setInitialTime] = useState('');
  const [creditLimit, setCreditLimit] = useState(account?.creditLimit != null ? String(account.creditLimit) : '');
  const [idealLimit, setIdealLimit] = useState(account?.idealCreditLimit != null ? String(account.idealCreditLimit) : '');
  const [sort, setSort] = useState(String(account?.sort ?? 0));
  const [status, setStatus] = useState(account?.status ?? 1);
  const [remark, setRemark] = useState(account?.remark ?? '');
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const grouped = useMemo(() => {
    const map = new Map();
    for (const type of types) {
      const key = type.providerName || '未命名提供方';
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(type);
    }
    return [...map.entries()];
  }, [types]);

  const selectedType = types.find((t) => t.id === typeId);
  const credit = isCredit(selectedType?.typeCode);
  // 资产和信用账户的余额含义相反，编辑时不允许跨性质换类型（后端同样会拦）
  const lockedCredit = editing ? isCredit(account.typeCode) : null;

  const errors = {};
  if (!typeId) errors.type = '请选择账户类型';
  if (tail && !/^\d{4}$/.test(tail)) errors.tail = '尾号必须是 4 位数字';
  if (!editing && initialTime && initialTime > toInputDateTime(new Date())) errors.time = '不能晚于当前时间';
  const valid = Object.keys(errors).length === 0;

  async function submit(event) {
    event.preventDefault();
    setSubmitted(true);
    if (!valid || busy) return;
    setBusy(true);
    const dto = {
      typeId,
      accTailNum: tail || null,
      creditLimit: credit && creditLimit !== '' ? creditLimit : null,
      idealCreditLimit: credit && idealLimit !== '' ? idealLimit : null,
      sort: Number(sort) || 0,
      status,
      remark: remark.trim() || null,
    };
    try {
      if (editing) {
        await api.updateAccount({ ...dto, id: account.id });
      } else {
        await api.createAccount({
          ...dto,
          initialBalance: initialBalance || null,
          initialTransactionTime: fromInputDateTime(initialTime),
        });
        bumpLedger();
      }
      await refreshAccounts();
      toast(editing ? '账户已更新' : '账户已创建');
      onClose();
    } catch (error) {
      toast(error.message, 'error');
      setBusy(false);
    }
  }

  const show = (key) => submitted && errors[key];

  return (
    <Modal title={editing ? '编辑账户' : '新建账户'} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'grid', gap: 18 }} noValidate>
        <Field
          label="账户类型"
          error={show('type') && errors.type}
          hint={editing ? '只能在同为资产或同为信用的类型之间更换' : undefined}
        >
          <select className="control" value={typeId} onChange={(e) => setTypeId(e.target.value)}>
            <option value="">选择提供方和类型</option>
            {grouped.map(([providerName, list]) => (
              <optgroup key={providerName} label={providerName}>
                {list.map((type) => (
                  <option
                    key={type.id}
                    value={type.id}
                    disabled={lockedCredit !== null && isCredit(type.typeCode) !== lockedCredit}
                  >
                    {type.typeName} · {isCredit(type.typeCode) ? '信用' : '资产'}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
        </Field>

        <div className="field-row">
          <Field label="账户尾号" aside="选填 · 4 位" error={show('tail') && errors.tail}>
            <input
              className="control num"
              inputMode="numeric"
              maxLength={4}
              placeholder="如 1234"
              value={tail}
              onChange={(e) => setTail(e.target.value.replace(/\D/g, '').slice(0, 4))}
            />
          </Field>
          <Field label="账户名预览">
            <div className="control" style={{ display: 'flex', alignItems: 'center', color: 'var(--ink-soft)', background: 'var(--paper)' }}>
              {previewName(selectedType, tail) || '—'}
            </div>
          </Field>
        </div>

        {!editing && (
          <div className="field-row">
            <Field label={credit ? '初始欠款' : '初始余额'} aside="选填">
              <input
                className="control num"
                inputMode="decimal"
                placeholder="0.00"
                value={initialBalance}
                onChange={(e) => setInitialBalance(sanitizeAmount(e.target.value))}
              />
            </Field>
            <Field label="初始资金时间" aside="默认现在" error={show('time') && errors.time}>
              <input
                type="datetime-local"
                className="control"
                value={initialTime}
                max={toInputDateTime(new Date())}
                onChange={(e) => setInitialTime(e.target.value)}
              />
            </Field>
          </div>
        )}

        {credit && (
          <div className="field-row">
            <Field label="信用额度" aside="选填">
              <input
                className="control num"
                inputMode="decimal"
                value={creditLimit}
                onChange={(e) => setCreditLimit(sanitizeAmount(e.target.value))}
              />
            </Field>
            <Field label="理想额度" aside="用量警戒线">
              <input
                className="control num"
                inputMode="decimal"
                value={idealLimit}
                onChange={(e) => setIdealLimit(sanitizeAmount(e.target.value))}
              />
            </Field>
          </div>
        )}

        <div className="field-row">
          <Field label="显示顺序" aside="越小越靠前">
            <input
              className="control num"
              inputMode="numeric"
              value={sort}
              onChange={(e) => setSort(e.target.value.replace(/\D/g, ''))}
            />
          </Field>
          <div className="field">
            <span className="field-label">状态</span>
            <div className="toggle" role="group" aria-label="账户状态">
              <button type="button" className={status === 1 ? 'active' : ''} onClick={() => setStatus(1)}>
                正常
              </button>
              <button type="button" className={status === 0 ? 'active' : ''} onClick={() => setStatus(0)}>
                停用
              </button>
            </div>
          </div>
        </div>

        <Field label="备注" aside="选填">
          <input className="control" maxLength={256} value={remark} onChange={(e) => setRemark(e.target.value)} />
        </Field>

        <div className="modal-foot">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            取消
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy && <LoaderCircle size={16} className="spin" />}
            {editing ? '保存' : '创建账户'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
