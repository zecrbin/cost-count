// 后端接口封装。统一响应为 { code, message, data }，code 不为 200 时抛出带中文原因的错误。

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

async function request(path, { method = 'GET', body } = {}) {
  let response;
  const isForm = body instanceof FormData;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      // FormData 由浏览器自动设置 multipart 边界，不能手动指定 Content-Type。
      headers: body === undefined || isForm ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined || isForm ? body : JSON.stringify(body),
    });
  } catch {
    throw new Error('无法连接记账服务，请确认后端已启动');
  }
  const result = await response.json().catch(() => null);
  if (!response.ok || result?.code !== 200) {
    throw new Error(result?.message || `请求失败（${response.status}）`);
  }
  return result.data;
}

const crud = (base) => ({
  list: (query = {}) => request(`${base}/list`, { method: 'POST', body: query }),
  get: (id) => request(`${base}/${id}`),
  create: (data) => request(base, { method: 'POST', body: data }),
  update: (data) => request(base, { method: 'PUT', body: data }),
  remove: (ids) => request(base, { method: 'DELETE', body: ids }),
});

export const api = {
  providers: crud('/api/account-providers'),
  accountTypes: crud('/api/account-types'),
  categories: crud('/api/categories'),
  accounts: {
    ...crud('/api/accounts'),
    adjustInitialBalance: (id, data) => request(`/api/accounts/${id}/initial-balance`, { method: 'PUT', body: data }),
  },
  dailyBalances: (query) => request('/api/account-daily-balances/list', { method: 'POST', body: query }),
  uploadIcon: (file) => {
    const body = new FormData();
    body.append('file', file);
    return request('/api/icons', { method: 'POST', body });
  },
  transactions: {
    page: (params = {}, pageNum = 1, pageSize = 50) =>
      request('/api/transactions/page', { method: 'POST', body: { pageNum, pageSize, params } }),
    get: (id) => request(`/api/transactions/${id}`),
    create: (data) => request('/api/transactions', { method: 'POST', body: data }),
    update: (data) => request('/api/transactions', { method: 'PUT', body: data }),
    remove: (id) => request(`/api/transactions/${id}`, { method: 'DELETE' }),
  },
};

/** 读取某时间段内的全部流水（分批拉取，个人账本数据量可控）。 */
export async function fetchAllTransactions(params, pageSize = 500) {
  const records = [];
  for (let pageNum = 1; ; pageNum += 1) {
    const page = await api.transactions.page(params, pageNum, pageSize);
    records.push(...page.records);
    if (records.length >= page.total || page.records.length === 0) return records;
  }
}
