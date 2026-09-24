import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { TransactionModal } from '../components/TransactionModal';

// 全局流水编辑器：任何页面都可以打开"记一笔"或编辑某笔流水。

const EditorContext = createContext(null);

export function TransactionEditorProvider({ children }) {
  const [state, setState] = useState({ opened: false, transaction: null, defaults: null });

  const openTransaction = useCallback((transaction = null, defaults = null) => {
    setState({ opened: true, transaction, defaults });
  }, []);
  const close = useCallback(() => setState((current) => ({ ...current, opened: false })), []);
  const value = useMemo(() => ({ openTransaction }), [openTransaction]);

  return (
    <EditorContext.Provider value={value}>
      {children}
      <TransactionModal opened={state.opened} onClose={close} transaction={state.transaction} defaults={state.defaults} />
    </EditorContext.Provider>
  );
}

export function useTransactionEditor() {
  return useContext(EditorContext);
}
