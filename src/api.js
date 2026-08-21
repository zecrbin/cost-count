async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!response.ok) throw new Error(`请求失败：${response.status}`);
  const result = await response.json();
  if (result.code !== 200) throw new Error(result.message || '操作失败');
  return result.data;
}

export const api = {
  dashboard: () => request('/api/dashboard/summary'),
  accounts: () => request('/api/accounts/list'),
  saveAccount: (data, id) => request(id ? `/api/accounts/${id}` : '/api/accounts', { method: id ? 'PUT' : 'POST', body: JSON.stringify(data) }),
  categories: () => request('/api/categories/list'),
  saveCategory: (data, id) => request(id ? `/api/categories/${id}` : '/api/categories', { method: id ? 'PUT' : 'POST', body: JSON.stringify(data) }),
  transactions: (query = {}) => request('/api/transactions/page', { method: 'POST', body: JSON.stringify({ pageNum: 1, pageSize: 100, params: query }) }),
  createTransaction: (data) => request('/api/transactions', { method: 'POST', body: JSON.stringify(data) }),
  deleteTransaction: (id) => request(`/api/transactions/${id}`, { method: 'DELETE' }),
};
