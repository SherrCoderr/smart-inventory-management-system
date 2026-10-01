import api from './axios';

export const stockIn = (data) => api.post('/inventory/stock-in', data);
export const stockOut = (data) => api.post('/inventory/stock-out', data);
export const getTransactions = (params) => api.get('/inventory/transactions', { params });
export const getLowStock = (params) => api.get('/inventory/low-stock', { params });
export const getReorderRecommendations = () => api.get('/inventory/reorder-recommendations');
