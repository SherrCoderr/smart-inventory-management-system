import { createContext, useState, useEffect } from 'react';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('inventoryUser');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('inventoryToken'));

  const login = (newToken, newUser) => {
    localStorage.setItem('inventoryToken', newToken);
    localStorage.setItem('inventoryUser', JSON.stringify(newUser));
    setToken(newToken);
    setUser(newUser);
  };

  const logout = () => {
    localStorage.removeItem('inventoryToken');
    localStorage.removeItem('inventoryUser');
    setToken(null);
    setUser(null);
  };

  const isAdmin = () => user?.role === 'ADMIN';
  const isAuthenticated = () => !!token;

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAdmin, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
};
