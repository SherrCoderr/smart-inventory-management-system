import { Loader2 } from 'lucide-react';
import clsx from 'clsx';

export const Spinner = ({ className }) => (
  <div className="flex justify-center items-center p-4">
    <Loader2 className={clsx("h-6 w-6 animate-spin text-primary-600", className)} />
  </div>
);
