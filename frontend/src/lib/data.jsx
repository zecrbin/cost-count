import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api';

// 全局字典数据：账户、分类、机构、账户类型。流水由各页面按需查询，写入后通过 ledgerVersion 通知刷新。

const DataContext = createContext(null);

const loaders = {
  accounts: () => api.accounts.list({}),
  categories: () => api.categories.list({}),
  providers: () => api.providers.list({}),
  accountTypes: () => api.accountTypes.list({}),
};

export function DataProvider({ children }) {
  const [data, setData] = useState({ accounts: [], categories: [], providers: [], accountTypes: [] });
  const [status, setStatus] = useState({ loading: true, error: null });
  const [ledgerVersion, setLedgerVersion] = useState(0);

  const reload = useCallback(async (...keys) => {
    const targets = keys.length ? keys : Object.keys(loaders);
    const results = await Promise.all(targets.map((key) => loaders[key]()));
    setData((previous) => ({ ...previous, ...Object.fromEntries(targets.map((key, index) => [key, results[index]])) }));
  }, []);

  const loadAll = useCallback(async () => {
    setStatus({ loading: true, error: null });
    try {
      await reload();
      setStatus({ loading: false, error: null });
    } catch (error) {
      setStatus({ loading: false, error });
    }
  }, [reload]);

  useEffect(() => { loadAll(); }, [loadAll]);

  /** 流水新增、修改、删除后调用：账户余额随之变化，各页面的流水查询也需要刷新。 */
  const ledgerChanged = useCallback(async () => {
    setLedgerVersion((version) => version + 1);
    await reload('accounts');
  }, [reload]);

  const value = useMemo(() => {
    const accountMap = new Map(data.accounts.map((account) => [account.id, account]));
    const categoryMap = new Map();
    for (const root of data.categories) {
      categoryMap.set(root.id, { ...root, root });
      for (const child of root.children || []) categoryMap.set(child.id, { ...child, root, parent: root });
    }
    return { ...data, ...status, reload, loadAll, ledgerVersion, ledgerChanged, accountMap, categoryMap };
  }, [data, status, reload, loadAll, ledgerVersion, ledgerChanged]);

  return <DataContext.Provider value={value}>{children}</DataContext.Provider>;
}

export function useData() {
  return useContext(DataContext);
}
