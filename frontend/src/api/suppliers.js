import api from './axios';

export const getSuppliers = (params) => api.get('/suppliers', { params });
export const getActiveSuppliers = () => api.get('/suppliers/active');
export const getSupplierById = (id) => api.get(`/suppliers/${id}`);
export const createSupplier = (data) => api.post('/suppliers', data);
export const updateSupplier = (id, data) => api.put(`/suppliers/${id}`, data);
export const toggleStatus = (id) => api.patch(`/suppliers/${id}/toggle-status`);
export const deleteSupplier = (id) => api.delete(`/suppliers/${id}`);
