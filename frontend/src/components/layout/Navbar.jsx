import { Menu, LogOut } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLocation, Link } from 'react-router-dom';
import { Badge } from '../ui/Badge';

const PAGE_TITLES = {
  '/dashboard': 'Dashboard',
  '/products': 'Products',
  '/categories': 'Categories',
  '/suppliers': 'Suppliers',
  '/inventory': 'Stock Operations',
  '/transactions': 'Transaction History',
  '/low-stock': 'Low Stock Alerts',
  '/reorder': 'Reorder Recommendations',
  '/profile': 'Profile',
};

export const Navbar = ({ onMenuClick }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const title = PAGE_TITLES[location.pathname] || 'SmartInventory';

  const initials = [user?.firstName?.charAt(0), user?.lastName?.charAt(0)]
    .filter(Boolean)
    .join('') || user?.username?.charAt(0)?.toUpperCase() || '?';

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 sm:px-6 z-30 relative">
      <div className="flex items-center">
        <button
          onClick={onMenuClick}
          className="md:hidden p-2 -ml-2 mr-2 text-gray-500 hover:text-gray-700 focus:outline-none"
          aria-label="Open sidebar"
        >
          <Menu className="h-6 w-6" />
        </button>
        <h2 className="text-xl font-semibold text-gray-800">{title}</h2>
      </div>

      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-3">
          <div className="hidden md:flex flex-col items-end">
            <span className="text-sm font-medium text-gray-900">{user?.username}</span>
            <Badge variant={user?.role === 'ADMIN' ? 'primary' : 'gray'}>{user?.role}</Badge>
          </div>
          <Link to="/profile" title="View Profile">
            <div className="h-9 w-9 rounded-full bg-primary-600 flex items-center justify-center text-white font-bold text-sm hover:bg-primary-700 transition-colors cursor-pointer">
              {initials}
            </div>
          </Link>
        </div>
        <button
          onClick={logout}
          className="p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100 transition-colors"
          title="Logout"
        >
          <LogOut className="h-5 w-5" />
        </button>
      </div>
    </header>
  );
};
