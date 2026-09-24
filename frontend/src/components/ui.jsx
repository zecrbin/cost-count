import { useEffect, useRef, useState } from 'react';
import { CircleAlert, LoaderCircle, X } from 'lucide-react';
import { useStore } from '../lib/store';
import { money, splitMoney } from '../lib/format';

export function Modal({ title, onClose, children, wide = false }) {
  const ref = useRef(null);

  useEffect(() => {
    const onKey = (event) => event.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    // 打开时聚焦第一个可输入的控件
    const first = ref.current?.querySelector('input:not([disabled]), select:not([disabled]), textarea');
    first?.focus();
    const { overflow } = document.body.style;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = overflow;
    };
  }, [onClose]);

  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal${wide ? ' wide' : ''}`} role="dialog" aria-modal="true" aria-label={title} ref={ref}>
        <div className="modal-head">
          <h2 className="modal-title">{title}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="关闭">
            <X size={18} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

export function Field({ label, hint, error, children, aside }) {
  return (
    <label className="field">
      {label && (
        <span className="field-label">
          {label}
          {aside && <small>{aside}</small>}
        </span>
      )}
      {children}
      {error ? <span className="hint error">{error}</span> : hint ? <span className="hint">{hint}</span> : null}
    </label>
  );
}

export function Toasts() {
  const { toasts } = useStore();
  return (
    <div className="toasts" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className={`toast${t.kind === 'error' ? ' error' : ''}`}>
          {t.kind === 'error' && <CircleAlert size={16} />}
          {t.message}
        </div>
      ))}
    </div>
  );
}

export function Loading({ label = '正在翻开账簿…' }) {
  return (
    <div className="loading">
      <LoaderCircle className="spin" size={22} />
      <span style={{ marginTop: 10 }}>{label}</span>
    </div>
  );
}

/** 一本摊开的账簿，空状态插画。 */
function LedgerArt() {
  return (
    <svg className="art" width="112" height="78" viewBox="0 0 112 78" fill="none" aria-hidden="true">
      <path d="M56 12C44 6 26 5 8 8v60c18-3 36-2 48 4V12Z" fill="#fbf8f2" stroke="#b3aa99" strokeWidth="1.5" />
      <path d="M56 12c12-6 30-7 48-4v60c-18-3-36-2-48 4V12Z" fill="#fbf8f2" stroke="#b3aa99" strokeWidth="1.5" />
      <path d="M18 24h28M18 32h28M18 40h20M66 24h28M66 32h22" stroke="#d9d1c2" strokeWidth="1.5" strokeLinecap="round" />
      <rect x="76" y="44" width="16" height="16" rx="3" fill="#b73a26" transform="rotate(-8 84 52)" />
    </svg>
  );
}

export function Empty({ title, children, action }) {
  return (
    <div className="empty">
      <LedgerArt />
      <strong>{title}</strong>
      {children && <span>{children}</span>}
      {action}
    </div>
  );
}

export function ConfirmDialog({ title, message, confirmText = '确认删除', onConfirm, onClose }) {
  const [busy, setBusy] = useState(false);
  const { toast } = useStore();

  async function handleConfirm() {
    setBusy(true);
    try {
      await onConfirm();
      onClose();
    } catch (error) {
      toast(error.message, 'error');
      setBusy(false);
    }
  }

  return (
    <Modal title={title} onClose={onClose}>
      <p style={{ margin: 0, color: 'var(--ink-soft)' }}>{message}</p>
      <div className="modal-foot">
        <button type="button" className="btn btn-ghost" onClick={onClose}>
          取消
        </button>
        <button type="button" className="btn btn-danger" onClick={handleConfirm} disabled={busy}>
          {busy && <LoaderCircle size={16} className="spin" />}
          {confirmText}
        </button>
      </div>
    </Modal>
  );
}

/** 金额数字：整数部分正常大小，小数部分弱化。 */
export function Money({ value, className = '', sign = '' }) {
  const { integer, fraction } = splitMoney(value);
  return (
    <span className={`num ${className}`}>
      {sign}
      {integer}
      <span style={{ opacity: 0.55 }}>.{fraction}</span>
    </span>
  );
}

export function plainMoney(value) {
  return money(value);
}

/** 数字从 0 滚到目标值，只在首次出现和数值变化时播放。 */
export function useCountUp(target, duration = 900) {
  const [value, setValue] = useState(0);
  const from = useRef(0);

  useEffect(() => {
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    // 页面不可见时浏览器会暂停 requestAnimationFrame，动画会停在起点，直接给出最终值
    if (reduce || document.hidden) {
      setValue(target);
      from.current = target;
      return undefined;
    }
    const start = performance.now();
    const origin = from.current;
    let frame;
    const tick = (now) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      setValue(origin + (target - origin) * eased);
      if (t < 1) frame = requestAnimationFrame(tick);
      else from.current = target;
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [target, duration]);

  return value;
}
