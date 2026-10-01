import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { DashboardLayout } from './layouts/DashboardLayout';

// Pages placeholders (will implement shortly)
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ProductsPage from './pages/ProductsPage';
import CategoriesPage from './pages/CategoriesPage';
import SuppliersPage from './pages/SuppliersPage';
import InventoryPage from './pages/InventoryPage';
import TransactionsPage from './pages/TransactionsPage';
import LowStockPage from './pages/LowStockPage';
import ReorderPage from './pages/ReorderPage';
import ProfilePage from './pages/ProfilePage';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        
        <Route element={<ProtectedRoute />}>
          <Route element={<DashboardLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/categories" element={<CategoriesPage />} />
            <Route path="/suppliers" element={<SuppliersPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/low-stock" element={<LowStockPage />} />
            <Route path="/reorder" element={<ReorderPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
