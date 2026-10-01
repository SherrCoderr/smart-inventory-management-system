import axios from 'axios';

// Normalize base URL to ensure no trailing slash
const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const baseURL = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('inventoryToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const isAuthEndpoint =
      error.config?.url?.includes('/auth/login') ||
      error.config?.url?.includes('/auth/register');

    if (error.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('inventoryToken');
      localStorage.removeItem('inventoryUser');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
