const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

async function request(path, options = {}) {
  try {
    const isFormData = options.body instanceof FormData;
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: { ...(isFormData ? {} : { 'Content-Type': 'application/json' }), ...options.headers },
      ...options,
    });
    const result = await response.json().catch(() => null);
    if (!response.ok || result?.code !== 200) {
      throw new Error(result?.message || `请求失败：${response.status}`);
    }
    return result.data;
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('无法连接记账服务，请确认后端已经启动');
    }
    throw error;
  }
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
  previewExcel: (file) => { const body = new FormData(); body.append('file', file); return request('/api/bill-import/excel/preview', { method: 'POST', body }); },
  recognizeImages: (files) => { const body = new FormData(); files.forEach(file => body.append('files', file)); return request('/api/bill-import/image/recognize', { method: 'POST', body }); },
  confirmImport: (rows) => request('/api/bill-import/confirm', { method: 'POST', body: JSON.stringify(rows) }),
};
