import clsx from 'clsx';

export const Badge = ({ children, variant = 'gray', className }) => {
  const variants = {
    gray: 'bg-gray-100 text-gray-800',
    green: 'bg-green-100 text-green-800',
    red: 'bg-red-100 text-red-800',
    yellow: 'bg-yellow-100 text-yellow-800',
    blue: 'bg-blue-100 text-blue-800',
    primary: 'bg-primary-100 text-primary-800',
  };
  return (
    <span className={clsx("inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold", variants[variant], className)}>
      {children}
    </span>
  );
};
