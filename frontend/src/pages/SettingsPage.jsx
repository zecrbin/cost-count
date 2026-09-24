import { useMemo, useState } from 'react';
import { Info, Pencil, Plus, Trash2 } from 'lucide-react';
import { ProviderAvatar } from '../components/AccountAvatar';
import { Empty } from '../components/ui';
import { useStore } from '../lib/store';
import { api } from '../lib/api';
import { isCredit } from '../lib/format';

function NatureTag({ code }) {
  const credit = isCredit(code);
  return (
    <span className={`tag ${credit ? 'tone-seal' : 'tone-jade'}`}>
      {credit ? '信用' : '资产'}
      <span style={{ opacity: 0.6 }}>{code}</span>
    </span>
  );
}

function ProvidersTab({ ui }) {
  const { providers, types, refreshDictionaries, toast } = useStore();
  const typeCount = useMemo(() => {
    const map = new Map();
    for (const t of types) map.set(t.providerId, (map.get(t.providerId) ?? 0) + 1);
    return map;
  }, [types]);

  function remove(provider) {
    ui.confirm({
      title: '删除提供方',
      message: `删除「${provider.providerName}」？提供方下还有账户类型时无法删除。`,
      onConfirm: async () => {
        await api.deleteProviders([provider.id]);
        await refreshDictionaries();
        toast('提供方已删除');
      },
    });
  }

  return (
    <div className="provider-grid">
      {providers.map((provider, i) => (
        <div className="sheet provider-tile reveal" style={{ '--i': i }} key={provider.id}>
          <ProviderAvatar provider={provider} />
          <div className="grow">
            <div className="name">{provider.providerName}</div>
            <div className="meta">{typeCount.get(provider.id) ?? 0} 个账户类型</div>
          </div>
          <div className="row-actions">
            <button type="button" className="icon-btn" onClick={() => ui.editProvider(provider)} aria-label="编辑提供方">
              <Pencil size={15} />
            </button>
            <button type="button" className="icon-btn danger" onClick={() => remove(provider)} aria-label="删除提供方">
              <Trash2 size={15} />
            </button>
          </div>
        </div>
      ))}
      <button
        type="button"
        className="add-tile reveal"
        style={{ minHeight: 78, '--i': providers.length }}
        onClick={() => ui.editProvider(null)}
      >
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
          <Plus size={17} />
          添加提供方
        </span>
      </button>
    </div>
  );
}

function TypesTab({ ui }) {
  const { providers, types, refreshDictionaries, toast } = useStore();
  const grouped = useMemo(
    () => providers.map((provider) => ({ provider, list: types.filter((t) => t.providerId === provider.id) })),
    [providers, types],
  );

  function remove(type) {
    ui.confirm({
      title: '删除账户类型',
      message: `删除「${type.providerName} · ${type.typeName}」？已有账户使用的类型无法删除。`,
      onConfirm: async () => {
        await api.deleteTypes([type.id]);
        await refreshDictionaries();
        toast('账户类型已删除');
      },
    });
  }

  if (providers.length === 0) {
    return (
      <div className="sheet">
        <Empty title="还没有提供方">先在「提供方」里添加银行或支付平台</Empty>
      </div>
    );
  }

  return (
    <div>
      {grouped.map(({ provider, list }, i) => (
        <section className="sheet type-group reveal" style={{ '--i': i }} key={provider.id}>
          <div className="type-group-head">
            <ProviderAvatar provider={provider} size={30} />
            {provider.providerName}
            <button
              type="button"
              className="btn btn-ghost"
              style={{ marginLeft: 'auto', height: 32, padding: '0 12px', fontSize: 13 }}
              onClick={() => ui.editType(null, provider.id)}
            >
              <Plus size={14} />
              添加类型
            </button>
          </div>
          {list.length === 0 ? (
            <div className="type-row" style={{ color: 'var(--muted)' }}>
              暂无账户类型
            </div>
          ) : (
            list.map((type) => (
              <div className="type-row" key={type.id}>
                <span style={{ fontWeight: 500 }}>{type.typeName}</span>
                <NatureTag code={type.typeCode} />
                <span className="hint num">排序 {type.sort ?? 0}</span>
                <div className="row-actions">
                  <button type="button" className="icon-btn" onClick={() => ui.editType(type)} aria-label="编辑账户类型">
                    <Pencil size={15} />
                  </button>
                  <button type="button" className="icon-btn danger" onClick={() => remove(type)} aria-label="删除账户类型">
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            ))
          )}
        </section>
      ))}
    </div>
  );
}

function CategoriesTab() {
  const { categoryIndex } = useStore();
  const columns = [
    ['EXPENSE', '支出分类', 'tone-ink'],
    ['INCOME', '收入分类', 'tone-jade'],
  ];
  return (
    <>
      <div className="category-cols">
        {columns.map(([key, title, tone], i) => (
          <section className="sheet category-col reveal" style={{ '--i': i }} key={key}>
            <h3>
              {title}
              <span className={`tag ${tone}`}>{categoryIndex.tree[key]?.length ?? 0} 组</span>
            </h3>
            {(categoryIndex.tree[key] ?? []).map((group) => (
              <div className="category-group" key={group.id}>
                <span className="group-name">{group.categoryName}</span>
                <div className="chips">
                  {group.children.map((child) => (
                    <span className="chip static" key={child.id}>
                      {child.categoryName}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </section>
        ))}
      </div>
      <div className="note">
        <Info size={14} style={{ flex: 'none', marginTop: 2 }} />
        预置分类，记账时一级、二级分类都可以直接选；分类的增删改接口暂未开放。
      </div>
    </>
  );
}

const TABS = [
  ['providers', '提供方'],
  ['types', '账户类型'],
  ['categories', '收支分类'],
];

export function SettingsPage({ ui }) {
  const [tab, setTab] = useState('providers');
  return (
    <div className="page">
      <header className="page-head reveal">
        <div>
          <div className="kicker">DICTIONARY</div>
          <h1 className="page-title">账户字典</h1>
          <p className="page-desc">提供方和类型决定账户的名称、图标和性质，修改后会同步到相关账户</p>
        </div>
      </header>
      <div className="tabs reveal" style={{ '--i': 1 }} role="tablist">
        {TABS.map(([key, label]) => (
          <button type="button" key={key} role="tab" aria-selected={tab === key} className={tab === key ? 'active' : ''} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </div>
      {tab === 'providers' && <ProvidersTab ui={ui} />}
      {tab === 'types' && <TypesTab ui={ui} />}
      {tab === 'categories' && <CategoriesTab />}
    </div>
  );
}
