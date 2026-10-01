import clsx from 'clsx';

export const Button = ({ children, variant = 'primary', className, ...props }) => {
  const base = 'inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none';
  const variants = {
    primary: 'bg-primary-600 text-white hover:bg-primary-700',
    secondary: 'bg-gray-100 text-gray-900 hover:bg-gray-200',
    danger: 'bg-red-600 text-white hover:bg-red-700',
    outline: 'border border-gray-300 bg-transparent hover:bg-gray-50 text-gray-700',
  };
  const sizes = {
    sm: 'h-8 px-3 text-xs',
    md: 'h-10 py-2 px-4',
    lg: 'h-11 px-8',
  };
  
  return (
    <button className={clsx(base, variants[variant], sizes.md, className)} {...props}>
      {children}
    </button>
  );
};
