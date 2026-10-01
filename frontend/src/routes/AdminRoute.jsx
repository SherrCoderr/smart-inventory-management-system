import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export const AdminRoute = () => {
  const { isAdmin } = useAuth();
  return isAdmin() ? <Outlet /> : <Navigate to="/dashboard" replace />;
};
