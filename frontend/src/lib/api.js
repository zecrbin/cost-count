// 后端统一返回 { code, message, data }，业务失败时 HTTP 状态仍是 200，要看 code 判断。
// 雪花 ID 超出 JS 安全整数范围，后端已序列化成字符串，前端全程按字符串传递，不要转数字。

async function request(method, path, body) {
  let response;
  try {
    response = await fetch(path, {
      method,
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new Error('无法连接服务，请确认后端已启动');
  }

  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error(`服务异常（HTTP ${response.status}）`);
  }
  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败');
  }
  return payload.data;
}

export const api = {
  listAccounts: (query = {}) => request('POST', '/api/accounts/list', query),
  createAccount: (dto) => request('POST', '/api/accounts', dto),
  updateAccount: (dto) => request('PUT', '/api/accounts', dto),
  deleteAccounts: (ids) => request('DELETE', '/api/accounts', ids),
  updateBalance: (dto) => request('POST', '/api/accounts/updateBalance', dto),

  pageTransactions: (pageNum, pageSize, params) =>
    request('POST', '/api/transactions/page', { pageNum, pageSize, params }),
  createTransaction: (dto) => request('POST', '/api/transactions', dto),

  listCategories: () => request('POST', '/api/categories/list', {}),

  listProviders: () => request('POST', '/api/account-providers/list', {}),
  createProvider: (dto) => request('POST', '/api/account-providers', dto),
  updateProvider: (dto) => request('PUT', '/api/account-providers', dto),
  deleteProviders: (ids) => request('DELETE', '/api/account-providers', ids),

  listTypes: () => request('POST', '/api/account-types/list', {}),
  createType: (dto) => request('POST', '/api/account-types', dto),
  updateType: (dto) => request('PUT', '/api/account-types', dto),
  deleteTypes: (ids) => request('DELETE', '/api/account-types', ids),
};
