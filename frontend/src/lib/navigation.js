export const PAGE_IDS = ['dashboard', 'transactions', 'accounts', 'categories', 'settings'];

export function currentPageId() {
  const id = window.location.hash.replace(/^#\/?/, '').split('?')[0];
  return PAGE_IDS.includes(id) ? id : 'dashboard';
}

/** 基于 hash 的页面切换，刷新后停留在当前页。 */
export function navigate(id) {
  window.location.hash = `/${id}`;
}
