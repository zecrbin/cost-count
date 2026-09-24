import { useState } from 'react';
import { Info, LoaderCircle } from 'lucide-react';
import { Field, Modal, Money } from '../components/ui';
import { AccountAvatar } from '../components/AccountAvatar';
import { useStore } from '../lib/store';
import { api } from '../lib/api';
import { isCredit, sanitizeAmount, signedMoney } from '../lib/format';

export function BalanceForm({ account, onClose }) {
  const { refreshAccounts, bumpLedger, toast } = useStore();
  const credit = isCredit(account.typeCode);
  const [target, setTarget] = useState(Number(account.balance || 0).toFixed(2));
  const [busy, setBusy] = useState(false);

  const parsed = Number(target);
  const valid = target !== '' && target !== '-' && Number.isFinite(parsed);
  const diff = valid ? Math.round((parsed - Number(account.balance || 0)) * 100) / 100 : 0;

  async function submit(event) {
    event.preventDefault();
    if (!valid || diff === 0 || busy) return;
    setBusy(true);
    try {
      await api.updateBalance({ accountId: account.id, currentBalance: target });
      await refreshAccounts();
      bumpLedger();
      toast('余额已修正');
      onClose();
    } catch (error) {
      toast(error.message, 'error');
      setBusy(false);
    }
  }

  return (
    <Modal title="修正余额" onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'grid', gap: 18 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <AccountAvatar account={account} size={46} />
          <div>
            <div className="account-name">{account.accName}</div>
            <div className="account-meta">
              账面{credit ? '待还' : '余额'} <Money value={account.balance} />
            </div>
          </div>
        </div>

        <Field label={credit ? '实际待还金额' : '实际余额'}>
          <input
            className="control num"
            style={{ height: 50, fontSize: 22 }}
            inputMode="decimal"
            value={target}
            onChange={(e) => setTarget(sanitizeAmount(e.target.value, true))}
          />
        </Field>

        <div className="preview-line">
          <span>{diff === 0 ? '和账面一致，不需要修正' : '将记一笔余额调整'}</span>
          {diff !== 0 && <span className="num" style={{ color: 'var(--gold)' }}>{signedMoney(diff)}</span>}
        </div>

        <div className="note" style={{ marginTop: -6 }}>
          <Info size={14} style={{ flex: 'none', marginTop: 2 }} />
          差额会以一笔当前时间的余额调整（ADJUSTMENT）入账，流水里可以查到。
        </div>

        <div className="modal-foot">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            取消
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy || !valid || diff === 0}>
            {busy && <LoaderCircle size={16} className="spin" />}
            确认修正
          </button>
        </div>
      </form>
    </Modal>
  );
}
