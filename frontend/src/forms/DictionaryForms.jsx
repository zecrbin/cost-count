import { useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { Field, Modal } from '../components/ui';
import { ProviderAvatar } from '../components/AccountAvatar';
import { useStore } from '../lib/store';
import { api } from '../lib/api';

/** 提供方或类型变更会重绘其下账户的名称和图标，保存后账户也要一起刷新。 */
function useDictionarySaved() {
  const { refreshDictionaries, refreshAccounts } = useStore();
  return async () => {
    await Promise.all([refreshDictionaries(), refreshAccounts()]);
  };
}

export function ProviderForm({ provider, onClose }) {
  const { providers, toast } = useStore();
  const saved = useDictionarySaved();
  const editing = Boolean(provider);
  const [name, setName] = useState(provider?.providerName ?? '');
  const [icon, setIcon] = useState(provider?.icon ?? '');
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const error = !name.trim() ? '请输入提供方名称' : null;
  const iconOptions = [...new Set(providers.map((p) => p.icon).filter(Boolean))];

  async function submit(event) {
    event.preventDefault();
    setSubmitted(true);
    if (error || busy) return;
    setBusy(true);
    try {
      const dto = { providerName: name.trim(), icon: icon.trim() || null };
      if (editing) await api.updateProvider({ ...dto, id: provider.id });
      else await api.createProvider(dto);
      await saved();
      toast(editing ? '提供方已更新，相关账户已同步' : '提供方已添加');
      onClose();
    } catch (err) {
      toast(err.message, 'error');
      setBusy(false);
    }
  }

  return (
    <Modal title={editing ? '编辑提供方' : '添加提供方'} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'grid', gap: 18 }} noValidate>
        <Field label="名称" error={submitted && error}>
          <input className="control" maxLength={128} value={name} onChange={(e) => setName(e.target.value)} placeholder="如 招商银行" />
        </Field>
        <Field label="图标路径" aside="相对 /icons/default/" hint="账户图标会以它为底图叠加尾号；修改后已有账户的图标会自动重绘">
          <div className="icon-preview">
            <ProviderAvatar provider={{ providerName: name || '新', icon: icon.trim() }} size={46} />
            <input
              className="control"
              list="provider-icons"
              maxLength={256}
              value={icon}
              onChange={(e) => setIcon(e.target.value)}
              placeholder="providers/cmb.png"
            />
            <datalist id="provider-icons">
              {iconOptions.map((path) => (
                <option key={path} value={path} />
              ))}
            </datalist>
          </div>
        </Field>
        <div className="modal-foot">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            取消
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy && <LoaderCircle size={16} className="spin" />}
            保存
          </button>
        </div>
      </form>
    </Modal>
  );
}

export function TypeForm({ type, presetProviderId, onClose }) {
  const { providers, toast } = useStore();
  const saved = useDictionarySaved();
  const editing = Boolean(type);
  const [providerId, setProviderId] = useState(type?.providerId ?? presetProviderId ?? '');
  const [typeCode, setTypeCode] = useState(type?.typeCode ?? 'DEBIT');
  const [typeName, setTypeName] = useState(type?.typeName ?? '');
  const [sort, setSort] = useState(String(type?.sort ?? 0));
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const errors = {};
  if (!providerId) errors.provider = '请选择提供方';
  if (!typeName.trim()) errors.name = '请输入类型名称';

  async function submit(event) {
    event.preventDefault();
    setSubmitted(true);
    if (Object.keys(errors).length || busy) return;
    setBusy(true);
    try {
      const dto = { providerId, typeCode, typeName: typeName.trim(), sort: Number(sort) || 0 };
      if (editing) await api.updateType({ ...dto, id: type.id });
      else await api.createType(dto);
      await saved();
      toast(editing ? '账户类型已更新，相关账户已同步' : '账户类型已添加');
      onClose();
    } catch (err) {
      toast(err.message, 'error');
      setBusy(false);
    }
  }

  return (
    <Modal title={editing ? '编辑账户类型' : '添加账户类型'} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'grid', gap: 18 }} noValidate>
        <Field label="所属提供方" error={submitted && errors.provider}>
          <select className="control" value={providerId} onChange={(e) => setProviderId(e.target.value)}>
            <option value="">选择提供方</option>
            {providers.map((p) => (
              <option key={p.id} value={p.id}>
                {p.providerName}
              </option>
            ))}
          </select>
        </Field>
        <div className="field-row">
          <Field label="类型名称" error={submitted && errors.name}>
            <input className="control" maxLength={128} value={typeName} onChange={(e) => setTypeName(e.target.value)} placeholder="如 储蓄卡" />
          </Field>
          <Field label="显示顺序">
            <input className="control num" inputMode="numeric" value={sort} onChange={(e) => setSort(e.target.value.replace(/\D/g, ''))} />
          </Field>
        </div>
        <div className="field">
          <span className="field-label">
            账户性质 <small>{editing ? '创建后不可修改' : '决定余额的含义'}</small>
          </span>
          <div className="toggle" role="group" aria-label="账户性质">
            {[
              ['DEBIT', '资产 · 余额是可用资金'],
              ['CREDIT', '信用 · 余额是待还金额'],
            ].map(([code, label]) => (
              <button
                type="button"
                key={code}
                className={typeCode === code ? 'active' : ''}
                disabled={editing && typeCode !== code}
                onClick={() => setTypeCode(code)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
        <div className="modal-foot">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            取消
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy && <LoaderCircle size={16} className="spin" />}
            保存
          </button>
        </div>
      </form>
    </Modal>
  );
}
