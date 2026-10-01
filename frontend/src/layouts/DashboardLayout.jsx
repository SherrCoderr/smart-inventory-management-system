import { useState, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from '../components/layout/Sidebar';
import { Navbar } from '../components/layout/Navbar';
import { getDashboardStats } from '../api/dashboard';

export const DashboardLayout = () => {
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [lowStockCount, setLowStockCount] = useState(0);
  const location = useLocation();

  useEffect(() => {
    setIsMobileOpen(false);
  }, [location]);

  useEffect(() => {
    getDashboardStats()
      .then(res => {
        if (res.success && res.data) {
          setLowStockCount(res.data.lowStockProductCount);
        }
      })
      .catch(console.error);
  }, [location.pathname]); // Refresh stats when navigation occurs

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Mobile overlay */}
      {isMobileOpen && (
        <div
          className="fixed inset-0 bg-slate-900/50 z-30 md:hidden"
          onClick={() => setIsMobileOpen(false)}
        />
      )}

      <Sidebar isMobileOpen={isMobileOpen} lowStockCount={lowStockCount} />

      <div className="flex-1 md:ml-64 flex flex-col min-h-screen transition-all duration-300">
        <Navbar onMenuClick={() => setIsMobileOpen(true)} />
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-x-hidden">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
