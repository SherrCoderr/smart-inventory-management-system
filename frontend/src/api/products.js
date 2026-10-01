import api from './axios';

export const getProducts = (params) => api.get('/products', { params });
export const getProductById = (id) => api.get(`/products/${id}`);
export const getProductBySku = (sku) => api.get(`/products/sku/${sku}`);
export const createProduct = (data) => api.post('/products', data);
export const updateProduct = (id, data) => api.put(`/products/${id}`, data);
export const deleteProduct = (id) => api.delete(`/products/${id}`);
export const toggleStatus = (id) => api.patch(`/products/${id}/toggle-status`);
