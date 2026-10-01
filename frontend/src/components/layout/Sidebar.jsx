import { LayoutDashboard, Package, Tags, Users, ArrowRightLeft, History, AlertTriangle, ShoppingCart, User } from 'lucide-react';
import { SidebarLink } from './SidebarLink';

export const Sidebar = ({ isMobileOpen, lowStockCount }) => {
  return (
    <aside
      className={`fixed inset-y-0 left-0 z-40 w-64 bg-slate-900 transform transition-transform duration-300 ease-in-out md:translate-x-0 ${
        isMobileOpen ? 'translate-x-0' : '-translate-x-full'
      }`}
    >
      <div className="flex h-16 items-center px-6 bg-slate-950">
        <h1 className="text-xl font-bold text-white tracking-tight">SmartInventory</h1>
      </div>
      <div className="px-3 py-4 space-y-1 h-[calc(100vh-4rem)] overflow-y-auto">
        <SidebarLink to="/dashboard" icon={LayoutDashboard} label="Dashboard" />
        <SidebarLink to="/products" icon={Package} label="Products" />
        <SidebarLink to="/categories" icon={Tags} label="Categories" />
        <SidebarLink to="/suppliers" icon={Users} label="Suppliers" />
        <div className="pt-4 pb-2">
          <p className="px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Inventory</p>
        </div>
        <SidebarLink to="/inventory" icon={ArrowRightLeft} label="Stock In/Out" />
        <SidebarLink to="/transactions" icon={History} label="Transactions" />
        <SidebarLink to="/low-stock" icon={AlertTriangle} label="Low Stock" badge={lowStockCount} />
        <SidebarLink to="/reorder" icon={ShoppingCart} label="Reorder" />
        <div className="pt-4 pb-2">
          <p className="px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Settings</p>
        </div>
        <SidebarLink to="/profile" icon={User} label="Profile" />
      </div>
    </aside>
  );
};
