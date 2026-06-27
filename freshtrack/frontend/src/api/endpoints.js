import api from './client';

// ---- Auth ----
export const login = (usernameOrEmail, password) =>
  api.post('/auth/login', { usernameOrEmail, password }).then((r) => r.data);
export const fetchMe = () => api.get('/auth/me').then((r) => r.data);

// ---- Dashboard ----
export const getStats = () => api.get('/dashboard/stats').then((r) => r.data);
export const getMyWarehouses = () => api.get('/dashboard/my-warehouses').then((r) => r.data);

// ---- Admin: warehouses ----
export const listWarehouses = () => api.get('/admin/warehouses').then((r) => r.data);
export const createWarehouse = (payload) =>
  api.post('/admin/warehouses', payload).then((r) => r.data);

// ---- Admin: users ----
export const listUsers = () => api.get('/admin/users').then((r) => r.data);
export const createUser = (payload) => api.post('/admin/users', payload).then((r) => r.data);
export const mapWarehouses = (userId, warehouseCodes) =>
  api.put(`/admin/users/${userId}/warehouses`, { warehouseCodes }).then((r) => r.data);
export const setUserStatus = (userId, enabled) =>
  api.put(`/admin/users/${userId}/status?enabled=${enabled}`).then((r) => r.data);

// ---- Invoices ----
export const uploadInvoices = (file) => {
  const form = new FormData();
  form.append('file', file);
  return api
    .post('/invoices/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data);
};
export const listAllInvoices = () => api.get('/invoices').then((r) => r.data);
export const listInvoicesByWarehouse = (warehouseCode) =>
  api.get(`/invoices/by-warehouse/${encodeURIComponent(warehouseCode)}`).then((r) => r.data);
export const getInvoiceDetail = (id) => api.get(`/invoices/${id}`).then((r) => r.data);

// ---- Receiving ----
export const scan = (payload) => api.post('/receiving/scan', payload).then((r) => r.data);
export const adjust = (payload) => api.post('/receiving/adjust', payload).then((r) => r.data);

// ---- Reports ----
export const getReconciliation = (params) =>
  api.get('/reports/reconciliation', { params }).then((r) => r.data);
export const exportReport = (format, params) =>
  api.get(`/reports/reconciliation/export/${format}`, { params, responseType: 'blob' });

// ---- Audit ----
export const listAudit = (params) => api.get('/audit', { params }).then((r) => r.data);
