import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { api } from './api';

const StoreContext = createContext(null);

export function useStore() {
  return useContext(StoreContext);
}

/** 按 pid 把平铺的分类组装成两级树，同时建一张 id → 名称的索引，流水列表用它显示分类名。 */
function buildCategoryIndex(categories) {
  const tree = { EXPENSE: [], INCOME: [] };
  const byId = new Map();
  const groups = new Map();
  for (const item of categories) {
    if (item.pid === '0') {
      const group = { ...item, children: [] };
      groups.set(item.id, group);
      (tree[item.categoryType] ||= []).push(group);
    }
  }
  for (const item of categories) {
    if (item.pid !== '0') groups.get(item.pid)?.children.push(item);
  }
  for (const item of categories) {
    const parent = item.pid === '0' ? null : groups.get(item.pid);
    byId.set(item.id, { name: item.categoryName, parentName: parent?.categoryName ?? null });
  }
  return { tree, byId };
}

export function StoreProvider({ children }) {
  const [accounts, setAccounts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [providers, setProviders] = useState([]);
  const [types, setTypes] = useState([]);
  const [ready, setReady] = useState(false);
  const [offline, setOffline] = useState(null);
  const [toasts, setToasts] = useState([]);
  // 记账后递增，流水页据此重新拉取
  const [ledgerVersion, setLedgerVersion] = useState(0);
  const toastSeq = useRef(0);

  const toast = useCallback((message, kind = 'info') => {
    const id = ++toastSeq.current;
    setToasts((list) => [...list, { id, message, kind }]);
    setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), kind === 'error' ? 4200 : 2600);
  }, []);

  const refreshAccounts = useCallback(async () => {
    setAccounts(await api.listAccounts());
  }, []);

  const refreshDictionaries = useCallback(async () => {
    const [nextProviders, nextTypes] = await Promise.all([api.listProviders(), api.listTypes()]);
    setProviders(nextProviders);
    setTypes(nextTypes);
  }, []);

  const loadAll = useCallback(async () => {
    try {
      const [nextAccounts, nextCategories, nextProviders, nextTypes] = await Promise.all([
        api.listAccounts(),
        api.listCategories(),
        api.listProviders(),
        api.listTypes(),
      ]);
      setAccounts(nextAccounts);
      setCategories(nextCategories);
      setProviders(nextProviders);
      setTypes(nextTypes);
      setOffline(null);
    } catch (error) {
      setOffline(error.message);
    } finally {
      setReady(true);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const bumpLedger = useCallback(() => setLedgerVersion((v) => v + 1), []);

  const categoryIndex = useMemo(() => buildCategoryIndex(categories), [categories]);

  const value = {
    accounts,
    categories,
    categoryIndex,
    providers,
    types,
    ready,
    offline,
    loadAll,
    refreshAccounts,
    refreshDictionaries,
    ledgerVersion,
    bumpLedger,
    toast,
    toasts,
  };

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}
