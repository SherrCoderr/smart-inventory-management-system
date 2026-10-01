import { Link, useLocation } from 'react-router-dom';
import clsx from 'clsx';

export const SidebarLink = ({ to, icon: Icon, label, badge }) => {
  const location = useLocation();
  // Exact match for root, prefix match for all other routes
  const isActive = to === '/'
    ? location.pathname === '/'
    : location.pathname.startsWith(to);

  return (
    <Link
      to={to}
      className={clsx(
        "flex items-center px-4 py-3 text-sm font-medium rounded-md transition-colors",
        isActive
          ? "bg-slate-800 text-white"
          : "text-slate-300 hover:bg-slate-800 hover:text-white"
      )}
    >
      <Icon className="h-5 w-5 mr-3 flex-shrink-0" />
      <span className="flex-1">{label}</span>
      {badge !== undefined && badge > 0 && (
        <span className="bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">
          {badge}
        </span>
      )}
    </Link>
  );
};
